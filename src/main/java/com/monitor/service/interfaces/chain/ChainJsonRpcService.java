package com.monitor.service.interfaces.chain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.service.parameter.QueryParam;
import com.monitor.utils.HttpUtil;
import com.monitor.utils.TimeUtil;
import org.springframework.util.CollectionUtils;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.monitor.constants.Topics.TOPIC;
import static com.monitor.utils.CommonUtil.GET_AMOUNT_VALUE;
import static org.web3j.utils.Numeric.toHexStringWithPrefix;

public interface ChainJsonRpcService extends BlockchainService {

    /**
     * get json rpc url
     *
     * @return
     */
    String getJsonRpcURL(String chain);

    default Map<String, String> getTransactionByBlockNumber(String chain, String blockNumber, String transactionHash) {
        JSONObject payload = new JSONObject()
                .fluentPut("method", "eth_getBlockByNumber")
                .fluentPut("id", "1")
                .fluentPut("params", new Object[]{blockNumber.startsWith("0x") ? blockNumber : ("0x" + Long.toHexString(Long.parseLong(blockNumber))), true});

        JSONObject responseObject = JSON.parseObject(HttpUtil.post(getJsonRpcURL(chain), payload.toString()));
        JSONObject result = responseObject.getJSONObject("result");

        Map<String, String> resultMap = new HashMap<>();

        String timestamp = result.getString("timestamp");
        resultMap.put("timestamp", timestamp);
        resultMap.put("time", TimeUtil.FORMAT(new Date(Long.parseLong(String.valueOf(Long.parseLong(timestamp.substring(2), 16))) * 1000), true));

        JSONArray transactions = result.getJSONArray("transactions");
        transactions.stream().filter(tx -> transactionHash.equals(((JSONObject) tx).getString("hash")))
                .findFirst()
                .ifPresent(tx -> resultMap.put("from", ((JSONObject) tx).getString("from")));

        return resultMap;
    }

    default Map<String, String> getTransactionByHash(String chain, String transactionHash) {
        JSONObject payload = new JSONObject()
                .fluentPut("method", "eth_getTransactionByHash")
                .fluentPut("jsonrpc", "2.0")
                .fluentPut("id", "1")
                .fluentPut("params", new Object[]{transactionHash});

        JSONObject response = JSON.parseObject(HttpUtil.post(getJsonRpcURL(chain), payload.toString())).getJSONObject("result");

        Map<String, String> result = new HashMap<>();

        result.put("sender", response.getString("from"));
        String gas = response.getString("gas");
        long gasValue = Long.parseLong(gas.substring(2), 16);

        result.put("gas", String.valueOf(gasValue));
        String gasPrice = response.getString("gasPrice");
        long gasPriceValue = Long.parseLong(gasPrice.substring(2), 16);

        result.put("gasPrice", GET_AMOUNT_VALUE(new BigDecimal(gasPriceValue), "1000000000000000000", 18));
        result.put("nonce", response.getString("nonce"));
        result.put("value", response.getString("value"));

        return result;
    }

    default Map<String, String> getTransactionReceiptByHash(String chain, String transactionHash) {
        JSONObject payload = new JSONObject()
                .fluentPut("method", "eth_getTransactionReceipt")
                .fluentPut("jsonrpc", "2.0")
                .fluentPut("id", "1")
                .fluentPut("params", new Object[]{transactionHash});

        JSONObject response = JSON.parseObject(HttpUtil.post(getJsonRpcURL(chain), payload.toString())).getJSONObject("result");

        Map<String, String> result = new HashMap<>();
        String gasPrice = response.getString("gasUsed");
        long gasUsed = Long.parseLong(gasPrice.substring(2), 16);

        result.put("gasUsed", String.valueOf(gasUsed));
        String cumulativeGasUsed = response.getString("cumulativeGasUsed");
        result.put("cumulativeGasUsed", String.valueOf(Long.parseLong(cumulativeGasUsed.substring(2), 16)));

        return result;
    }

    default Integer getTransactionCount(QueryParam queryParam) {
        JSONObject payload = new JSONObject()
                .fluentPut("jsonrpc", "2.0")
                .fluentPut("method", "eth_getLogs")
                .fluentPut("id", "1");

        JSONObject param = new JSONObject()
                .fluentPut("address", queryParam.getContractAddress())
                .fluentPut("topics", TOPIC(queryParam.getTopic()));

        int sum = 0;
        BigInteger startBlock = new BigInteger(queryParam.getStartBlock().substring(2), 16);
        BigInteger endBlock = new BigInteger(queryParam.getEndBlock().substring(2), 16);
        BigInteger toBlock = startBlock.add(BigInteger.valueOf(queryParam.getOffset()));
        int offset = queryParam.getOffset();
        while (startBlock.compareTo(endBlock) <= 0) {
            param.fluentPut("fromBlock", toHexStringWithPrefix(startBlock.add(BigInteger.ONE)))
                    .fluentPut("toBlock", toHexStringWithPrefix(toBlock));

            JSONArray params = new JSONArray().fluentAdd(param);
            payload.put("params", params);

            String response = HttpUtil.post(getJsonRpcURL(queryParam.getChain()), payload.toString());
            JSONObject object = JSONObject.parseObject(response);

            if (object == null) {
                return sum;
            }
            if (object.getJSONObject("error") != null
                    && ("-32005".equals(object.getJSONObject("error").getString("code")) || "-32600".equals(object.getJSONObject("error").getString("code")))) {
                offset = offset / 2;
                toBlock = startBlock.add(BigInteger.valueOf(offset));
                continue;
            }
            JSONArray transactions = object.getJSONArray("result");
            if (!CollectionUtils.isEmpty(transactions)) {
                sum += transactions.size();
            }
            startBlock = toBlock.add(BigInteger.ONE);
            toBlock = startBlock.add(BigInteger.valueOf(offset)).min(endBlock);
        }
        return sum;
    }

    default String getLatestBlockNumber(String chain) {
        JSONObject payload = new JSONObject()
                .fluentPut("jsonrpc", "2.0")
                .fluentPut("method", "eth_blockNumber")
                .fluentPut("id", "1");

        JSONArray params = new JSONArray();
        payload.put("params", params);

        String response = HttpUtil.post(getJsonRpcURL(chain), payload.toString());
        JSONObject object = JSONObject.parseObject(response);

        return object == null ? null : object.getString("result");
    }
}
