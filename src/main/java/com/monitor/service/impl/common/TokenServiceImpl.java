package com.monitor.service.impl.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.constants.Monitor;
import com.monitor.constants.Slack;
import com.monitor.constants.Web3Provider;
import com.monitor.service.ServiceContext;
import com.monitor.service.interfaces.SlackService;
import com.monitor.service.interfaces.TokenService;
import com.monitor.service.interfaces.chain.BlockchainService;
import com.monitor.service.parameter.QueryParam;
import com.monitor.service.parameter.TotalSupply;
import com.monitor.utils.HttpUtil;
import com.monitor.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.monitor.constants.Monitor.DECIMAL18;
import static com.monitor.constants.ThirdPartyService.*;
import static com.monitor.constants.Token.TUSD;

/**
 * All externally provided interfaces
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    @Autowired
    public Slack slack;

    @Autowired
    public Monitor monitor;

    @Autowired
    public TUSD token;

    @Autowired
    public Web3Provider web3Provider;

    @Autowired
    public ServiceContext serviceContext;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private SlackService slackService;

    @Override
    public String currentPrice(String symbol, String convert) {
        String url = COIN_MARKET_CURRENCY + "?symbol=" + symbol + "&convert=" + convert;
        Map<String, String> properties = Map.of("Accepts", "application/json", "X-CMC_PRO_API_KEY", "ec4fa893-3f2d-4a09-8ede-7b81253a72cf");

        try {
            String response = HttpUtil.get(url, properties, 3);

            if (response == null) {
                return "N/A";
            }
            JSONObject data = JSON.parseObject(response);
            Object tusdInfo = data.getJSONObject("data").getJSONArray(symbol).stream().filter(a -> ((JSONObject) a).getInteger("id") == 2563).findFirst().get();

            String price = ((JSONObject) tusdInfo).getJSONObject("quote").getJSONObject(convert).getString("price");

            redisUtil.saveStringValue("tusd_price", price, 0, null);

            return price;
        } catch (Exception exception) {
            slackService.sendDirectMessage("test", String.format("Get price failed %s", exception));
        }
        return redisUtil.getStringValueOrDefault("tusd_price", "1");
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
    public Map<String, BigDecimal> queryTotalSupplyByTimestamp(String chain, String time) {
        return this.queryTotalSupplyWithSummary();
    }

    @Override
    public Map<String, BigDecimal> queryTotalSupplyWithSummary() {
        ConcurrentHashMap<String, BigDecimal> supplies = new ConcurrentHashMap<>();

        List<CompletableFuture<BigDecimal>> futures = new ArrayList<>();
        for (Map.Entry<String, String> entry : web3Provider.getUrl().get("rpc").entrySet()) {
            String chain = entry.getKey();

            BigDecimal supply = new BigDecimal(redisUtil.getStringValueOrDefault(chain + "_totalSupply", "0"));
            supplies.put(chain, supply);
            if (supply.compareTo(BigDecimal.ZERO) > 0) {
                continue;
            }

            futures.add(CompletableFuture.supplyAsync(() -> this.calTotalSupply(chain)).exceptionally(e -> {
                log.error(e.getMessage(), e);
                return new BigDecimal(redisUtil.getStringValueOrDefault(chain + "_lastTotalSupply", "0"));
            }).whenCompleteAsync((response, error) -> supplies.put(chain, response)));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        supplies.put("time", new BigDecimal(System.currentTimeMillis()));

        for (Map.Entry<String, BigDecimal> entry : supplies.entrySet()) {
            redisUtil.saveStringValue(entry.getKey() + "_lastTotalSupply", entry.getValue().toString(), 0, null);
            redisUtil.saveStringValue(entry.getKey() + "_totalSupply", entry.getValue().toString(), 1, TimeUnit.MINUTES);
        }

        supplies.put("Natively Networks TotalSupply", supplies.entrySet().stream().filter(entry -> monitor.getNativelyChains().contains(entry.getKey())).map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add));
        supplies.put("Bridged Networks TotalSupply", supplies.entrySet().stream().filter(entry -> monitor.getBridgedChains().contains(entry.getKey())).map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add));

        return supplies;
    }

    private BigDecimal calTotalSupply(String chain) {
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
        QueryParam.QueryParamBuilder builder = QueryParam.builderFromQueryParam(queryParam).method("eth_getLogs").topic("MINT");
        return this.getSpecifiedTransactions(builder.build(), null);
    }

    @Override
    public Map<String, Map<String, String>> getRedemptionTransactions(QueryParam queryParam) {
        QueryParam.QueryParamBuilder builder = QueryParam.builderFromQueryParam(queryParam).address(token.getContract(queryParam.getChain())).method("eth_getLogs").topic("TRANSFER");
        return getSpecifiedTransactions(builder.build(), param -> {
            BigInteger address = (BigInteger) param;
            return address.compareTo(BigInteger.valueOf(1048576)) < 0 && address.compareTo(BigInteger.valueOf(0)) != 0;
        });
    }

    @Override
    public Map<String, Map<String, String>> getSpecifiedTransactions(QueryParam queryParam, Function<Object, Boolean> condition) {

        BlockchainService service = serviceContext.chainServiceOf(queryParam.getChain());
        Map<String, Map<String, String>> transactions = service.getTransactionsByEvent(queryParam);
        for (Map<String, String> transaction : transactions.values()) {

            String blockNumber = transaction.get("blockNumber");
            String trx_hash = transaction.get("transactionHash");

            transaction.putAll(service.getTransactionByBlockNumber(queryParam.getChain(), blockNumber, trx_hash));
            transaction.putAll(service.getTransactionByHash(queryParam.getChain(), trx_hash));
            transaction.putAll(service.getTransactionReceiptByHash(queryParam.getChain(), trx_hash));

            BigDecimal gasUsedValue = new BigDecimal(transaction.get("gasPrice")).multiply(new BigDecimal(transaction.get("gasUsed")));
            transaction.put("gasUsedValue", String.valueOf(gasUsedValue));
        }

        return transactions;
    }

    @Override
    public BigDecimal getBalance(String chain, String address) {
        return serviceContext.chainServiceOf(chain).getBalance(chain, address);
    }

    @Override
    public String ripcords() {
        String response = HttpUtil.get(RIPCORDS_URL);

        if (response == null) {
            throw new RuntimeException("Can not find reserve information.");
        }

        JSONObject data = JSON.parseObject(response);

        if (data == null || data.getString("ripcord") == null || data.getJSONArray("ripcordDetails") == null) {
            throw new RuntimeException("Can not find reserve information.");
        }

        JSONArray ripcordDetails = data.getJSONArray("ripcordDetails");

        if (data.getBoolean("ripcord") && ripcordDetails.isEmpty()) {
            throw new RuntimeException("Ripcord is true, but ripcord details is empty.");
        }

        if (!ripcordDetails.isEmpty()) {
            return ripcordDetails.getString(0);
        }

        return null;
    }

    @Override
    public JSONArray getLaunchPool() {
        String response = HttpUtil.get(BINANCE_LAUNCHPAD_URL);
        if (response == null) {
            return null;
        }
        JSONObject data = JSONObject.parseObject(response).getJSONObject("data");
        return data == null ? null : data.getJSONArray("tracking");
    }
}
