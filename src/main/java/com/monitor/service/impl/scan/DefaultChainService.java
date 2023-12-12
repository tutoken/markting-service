package com.monitor.service.impl.scan;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.constants.Web3Provider;
import com.monitor.service.interfaces.chain.BlockchainService;
import com.monitor.service.interfaces.chain.ChainJsonRpcService;
import com.monitor.service.interfaces.chain.ChainScanService;
import com.monitor.service.parameter.QueryParam;
import com.monitor.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

import static com.monitor.constants.Token.TUSD;

@Slf4j
@Service("defaultChainService")
public class DefaultChainService implements ChainScanService, ChainJsonRpcService {

    @Autowired
    protected Web3Provider web3Provider;

    @Autowired
    protected RedisUtil redisUtil;

    @Autowired
    protected TUSD token;

    protected String chain;

    public BlockchainService init(String chain) {
        this.chain = chain;
        return this;
    }

    @Override
    public String getJsonRpcURL() {
        return this.web3Provider.getRpcUrl(chain);
    }

    @Override
    public String getApiKey() {
        return this.web3Provider.getScanKey(chain);
    }

    @Override
    public String getBlockchainScanURL() {
        return this.web3Provider.getScanUrl(chain);
    }

    @Override
    public int getTransactionCount() {
        String lastCountKey = chain + "_total_transaction_last_count";
        String lastBlockKey = chain + "_total_transaction_last_time";

        int offset = 10000;
        int page = 1;
        BigInteger currentBlock = new BigInteger(redisUtil.getStringValueOrDefault(lastBlockKey, "0"));

        QueryParam.QueryParamBuilder queryParamBuilder = QueryParam.builder().contractAddress(token.getContract(chain)).endBlock("latest").offset(offset).page(page);

        int currentCount;
        int lastCount = Integer.parseInt(redisUtil.getStringValueOrDefault(lastCountKey, "0"));
        do {
            queryParamBuilder.startBlock(currentBlock.toString());
            JSONArray transactions = this.getERC20Transactions(queryParamBuilder.build());

            currentCount = transactions.size();
            log.info(String.format("%s current count is %d", this.chain, currentCount));
            if (currentCount == 0) {
                break;
            }
            lastCount += currentCount;
            BigInteger firstBlockNumber = ((JSONObject) transactions.get(0)).getBigInteger("blockNumber");
            BigInteger lastBlockNumber = ((JSONObject) transactions.get(transactions.size() - 1)).getBigInteger("blockNumber");

            currentBlock = currentBlock.max(firstBlockNumber.max(lastBlockNumber));

            // To avoid rate limit caused by frequent visiting
            if (currentCount >= offset) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        } while (currentCount >= offset);

        currentBlock = currentBlock.add(BigInteger.ONE);
        log.info(String.format("%s sum is %d", this.chain, lastCount));

        redisUtil.saveStringValue(lastCountKey, String.valueOf(lastCount), 0, null);
        redisUtil.saveStringValue(lastBlockKey, currentBlock.toString(), 0, null);

        return lastCount;
    }
}
