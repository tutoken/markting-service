package com.monitor.service.impl.scan;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.utils.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigInteger;

import static com.monitor.constants.Topics.TOPIC;
import static org.web3j.utils.Numeric.toHexStringWithPrefix;

@Slf4j
@Service("avaxChainService")
public class AvaxChainService extends DefaultChainService {

    @Override
    public int getTransactionCount() {
        String lastCountKey = chain + "_total_transaction_last_count";
        String lastBlockKey = chain + "_total_transaction_last_time";

        BigInteger startBlock = new BigInteger(redisUtil.getStringValueOrDefault(lastBlockKey, "0"));
        int lastCount = Integer.parseInt(redisUtil.getStringValueOrDefault(lastCountKey, "0"));

        JSONObject payload = new JSONObject().fluentPut("jsonrpc", "2.0")
                .fluentPut("method", "eth_getLogs").fluentPut("id", "1");

        JSONObject param = new JSONObject().fluentPut("address", token.getContract(chain))
                .fluentPut("topics", TOPIC("TRANSFER"));

        String latestBlock = getLatestBlockNumber();
        BigInteger endBlock = new BigInteger(latestBlock.substring(2), 16);

        int offset = 8000;
        while (startBlock.compareTo(endBlock) <= 0) {
            BigInteger toBlock = startBlock.add(BigInteger.valueOf(offset)).min(endBlock);
            param.fluentPut("fromBlock", toHexStringWithPrefix(startBlock))
                    .fluentPut("toBlock", toHexStringWithPrefix(toBlock));

            JSONArray params = new JSONArray().fluentAdd(param);
            payload.put("params", params);

            String response = HttpUtil.post(getJsonRpcURL(), payload.toString());
            JSONObject object = JSONObject.parseObject(response);

            if (object == null) {
                return lastCount;
            }
            if (object.getJSONObject("error") != null && ("-32701".equals(object.getJSONObject("error").getString("code")) || "-32600".equals(object.getJSONObject("error").getString("code")))) {
                offset /= 2;
                continue;
            } else {
                offset = 8000;
            }
            JSONArray transactions = object.getJSONArray("result");
            if (!CollectionUtils.isEmpty(transactions)) {
                lastCount += transactions.size();
            }
            startBlock = toBlock.add(BigInteger.ONE);
        }

        redisUtil.saveStringValue(lastCountKey, String.valueOf(lastCount), 0, null);
        redisUtil.saveStringValue(lastBlockKey, endBlock.add(BigInteger.ONE).toString(), 0, null);

        return lastCount;
    }
}
