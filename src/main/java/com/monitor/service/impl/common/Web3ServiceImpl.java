package com.monitor.service.impl.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.constants.Monitor;
import com.monitor.constants.Slack;
import com.monitor.constants.Topics;
import com.monitor.constants.Web3Provider;
import com.monitor.service.ServiceContext;
import com.monitor.service.interfaces.SlackService;
import com.monitor.service.interfaces.Web3Service;
import com.monitor.service.interfaces.chain.BlockchainService;
import com.monitor.service.parameter.QueryParam;
import com.monitor.service.parameter.TotalSupply;
import com.monitor.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.monitor.constants.Monitor.DECIMAL18;
import static com.monitor.constants.Token.TUSD;

/**
 * All externally provided interfaces
 */
@Slf4j
@Service
public class Web3ServiceImpl implements Web3Service {

    @Autowired
    public Slack slack;

    @Autowired
    public Monitor monitor;

    @Autowired
    public TUSD token;

    @Autowired
    public Web3Provider web3Provider;

    @Autowired
    public SlackService slackService;

    @Autowired
    public ServiceContext serviceContext;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public Set<Map<String, String>> getTransactionsByAddress(QueryParam queryParam) {
        Map<String, String> result = new HashMap<>();
        JSONArray transactions = serviceContext.chainServiceOf(queryParam.getChain()).getTransactionsByAddress(queryParam);

        for (int i = 0; i < transactions.size(); i++) {
            JSONObject transaction = transactions.getJSONObject(i);

            result.put("hash", transaction.getString("hash"));
            result.put("method", Topics.METHOD(transaction.getString("methodId")));
        }

        return Set.of(result);
    }

    @Override
    public Map<String, Map<String, String>> getSpecifiedTransactions(QueryParam queryParam) {

        BlockchainService service = serviceContext.chainServiceOf(queryParam.getChain());
        Map<String, Map<String, String>> transactions = service.getTransactionsByEvent(queryParam);
        for (Map<String, String> transaction : transactions.values()) {

            String blockNumber = transaction.get("blockNumber");
            String trx_hash = transaction.get("transactionHash");

            transaction.putAll(service.getTransactionByBlockNumber(blockNumber, trx_hash));
            transaction.putAll(service.getTransactionByHash(trx_hash));
            transaction.putAll(service.getTransactionReceiptByHash(trx_hash));

            BigDecimal gasUsedValue = new BigDecimal(transaction.get("gasPrice")).multiply(new BigDecimal(transaction.get("gasUsed")));
            transaction.put("gasUsedValue", String.valueOf(gasUsedValue));
        }

        return transactions;
    }

    @Override
    public BigDecimal getBalance(String chain, String address) {
        return serviceContext.chainServiceOf(chain).getBalance(address);
    }

    @Override
    public List<TotalSupply> getTotalSupply() {
        Map<String, BigDecimal> supplies = this.queryTotalSupplyWithSummary();
        List<TotalSupply> result = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : supplies.entrySet()) {
            result.add(new TotalSupply(entry.getKey(), entry.getValue()));
        }

        return result;
    }

    @Override
    public Map<String, BigDecimal> queryTotalSupplyWithSummary() {
        Map<String, BigDecimal> supplies = this.queryTotalSupply();

        for (Map.Entry<String, BigDecimal> entry : supplies.entrySet()) {
            redisUtil.saveStringValue(entry.getKey() + "_lastTotalSupply", entry.getValue().toString(), 0, null);
            redisUtil.saveStringValue(entry.getKey() + "_totalSupply", entry.getValue().toString(), 1, TimeUnit.MINUTES);
        }

        supplies.put("Natively Supply", supplies.entrySet().stream().filter(entry -> monitor.getNativelyChains().contains(entry.getKey())).map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add));
        supplies.put("Bridged Supply", supplies.entrySet().stream().filter(entry -> monitor.getBridgedChains().contains(entry.getKey())).map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add));

        return supplies;
    }

    @Override
    public Map<String, BigDecimal> queryTotalSupply() {
        ConcurrentHashMap<String, BigDecimal> supplies = new ConcurrentHashMap<>();

        List<CompletableFuture<BigDecimal>> futures = new ArrayList<>();
        for (Map.Entry<String, String> entry : web3Provider.getUrl().get("rpc").entrySet()) {
            String chain = entry.getKey();

            BigDecimal supply = new BigDecimal(redisUtil.getStringValueOrDefault(chain + "_totalSupply", "0"));
            supplies.put(chain, supply);
            if (supply.compareTo(BigDecimal.ZERO) > 0) {
                continue;
            }

            futures.add(CompletableFuture.supplyAsync(() -> this.cal(chain)).exceptionally(e -> {
                log.error(e.getMessage(), e);
                return new BigDecimal(redisUtil.getStringValueOrDefault(chain + "_lastTotalSupply", "0"));
            }).whenCompleteAsync((response, error) -> supplies.put(chain, response)));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        supplies.put("time", new BigDecimal(System.currentTimeMillis()));
        return supplies;
    }

    @Override
    public Map<String, BigDecimal> queryTotalSupplyByTimestamp(String chain, String timestamp) {
        String current_time = String.valueOf(System.currentTimeMillis());
        Map<String, BigDecimal> totalSupplies = this.queryTotalSupply();

        for (Map.Entry<String, BigDecimal> entry : totalSupplies.entrySet()) {
            QueryParam.QueryParamBuilder builder = QueryParam.builder().startTime(timestamp).endTime(current_time);
            QueryParam queryParam = builder.build();

            BigDecimal sumMint = this.getMintTransactions(queryParam).values().stream().map(transaction -> transaction.get("amount")).filter(Objects::nonNull).map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalSupply = totalSupplies.get(entry.getKey()).subtract(sumMint);

            totalSupply = totalSupply.add(this.getRedemptionTransactions(builder.build()).values().stream().map(transaction -> transaction.get("amount")).filter(Objects::nonNull).map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add));

            totalSupplies.put(entry.getKey(), totalSupply);
        }
        totalSupplies.put("time", new BigDecimal(System.currentTimeMillis()));

        return totalSupplies;
    }

    private BigDecimal cal(String chain) {
        try {
            BigDecimal value;
            if ("tron".equals(chain)) {
                value = new BigDecimal(new BigInteger(serviceContext.tusdServiceOf(chain).queryContract(chain, "totalSupply"), 16)).divide(DECIMAL18, 2, RoundingMode.HALF_UP);
            } else {
                value = new BigDecimal(serviceContext.tusdServiceOf(chain).queryContract(chain, "totalSupply"));
            }
            log.info(String.format("%s current total supply: %s", chain, value));

            return value;
        } catch (Exception e) {
            log.error(String.format("call TUSD total supply exception:%s", chain), e);

            return new BigDecimal(redisUtil.getStringValueOrDefault(chain + "_lastTotalSupply", "0"));
        }
    }

    @Override
    public Map<String, Map<String, String>> getMintTransactions(QueryParam queryParam) {
        QueryParam.QueryParamBuilder builder = QueryParam.builderFromQueryParam(queryParam).address(token.getContract(queryParam.getChain())).method("eth_getLogs").topic("MINT");
        return this.getSpecifiedTransactions(builder.build());
    }

    @Override
    public Map<String, Map<String, String>> getRedemptionTransactions(QueryParam queryParam) {
        QueryParam.QueryParamBuilder builder = QueryParam.builderFromQueryParam(queryParam).address(token.getContract(queryParam.getChain())).method("eth_getLogs").topic("TRANSFER");

        BlockchainService service = serviceContext.chainServiceOf(queryParam.getChain());
        Map<String, Map<String, String>> result = new HashMap<>();
        Map<String, Map<String, String>> transactions = service.getTransactionsByEvent(builder.build());
        for (Map<String, String> transaction : transactions.values()) {

            if (this.isRedemptionAddress((Numeric.toBigInt(transaction.get("to"))))) {
                String blockNumber = transaction.get("blockNumber");
                String trx_hash = transaction.get("transactionHash");

                transaction.putAll(service.getTransactionByBlockNumber(blockNumber, trx_hash));
                transaction.putAll(service.getTransactionByHash(trx_hash));
                transaction.putAll(service.getTransactionReceiptByHash(trx_hash));

                BigDecimal gasUsedValue = new BigDecimal(transaction.get("gasPrice")).multiply(new BigDecimal(transaction.get("gasUsed")));
                transaction.put("gasUsedValue", String.valueOf(gasUsedValue));
            }
        }
        return result;
    }

    private boolean isRedemptionAddress(BigInteger address) {
        return address.compareTo(BigInteger.valueOf(1048576)) < 0 && address.compareTo(BigInteger.valueOf(0)) != 0;
    }
}
