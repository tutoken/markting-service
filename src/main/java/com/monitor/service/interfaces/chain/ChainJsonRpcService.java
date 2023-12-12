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
    String getJsonRpcURL();

    default Map<String, Map<String, String>> getTransactionsByEvent(QueryParam queryParam) {
        JSONObject payload = new JSONObject()
                .fluentPut("jsonrpc", "2.0")
                .fluentPut("method", queryParam.getMethod())
                .fluentPut("id", "1");

        JSONObject param = new JSONObject()
                .fluentPut("address", queryParam.getAddress())
                .fluentPut("topics", TOPIC(queryParam.getTopic()))
                .fluentPut("fromBlock", toHexStringWithPrefix(new BigInteger(queryParam.getStartBlock())))
                .fluentPut("toBlock", toHexStringWithPrefix(new BigInteger(queryParam.getEndBlock())));

        JSONArray params = new JSONArray().fluentAdd(param);
        payload.put("params", params);

        String response = HttpUtil.post(getJsonRpcURL(), payload.toString());
        JSONObject object = JSONObject.parseObject(response);

        if (object == null) {
            return null;
        }

        JSONArray transactions = object.getJSONArray("result");
        if (CollectionUtils.isEmpty(transactions)) {
            return null;
        }
        Map<String, Map<String, String>> result = new HashMap<>();
        for (int i = 0; i < transactions.size(); i++) {
            JSONObject transaction = transactions.getJSONObject(i);
            Map<String, String> info = new HashMap<>();

            String blockNumber = transaction.getString("blockNumber");
            int blockNum = Integer.parseInt(blockNumber.substring(2), 16);

            String trx_hash = transaction.getString("transactionHash");

            info.put("blockNumber", String.valueOf(blockNum));
            info.put("transactionHash", trx_hash);
            info.put("blockHash", transaction.getString("blockHash"));

            // TODO for mint and redeem from to
            JSONArray topics = transaction.getJSONArray("topics");
            Object toAddress = topics.get(1);
            String to = toAddress == null ? "" : "0x" + toAddress.toString().substring(26);
            info.put("to", to);

            String data = (transaction.getString("data"));
            BigDecimal amount = data == null ? BigDecimal.ZERO : new BigDecimal(Numeric.toBigInt(transaction.getString("data")));
            info.put("amount", GET_AMOUNT_VALUE(amount, "1000000000000000000", 2));

            BigDecimal gasUsedValue = new BigDecimal(info.get("gasPrice")).multiply(new BigDecimal(info.get("gasUsed")));
            info.put("gasUsedValue", String.valueOf(gasUsedValue));

            result.put(trx_hash, info);
        }
        return result;
    }

    default JSONArray getTransactionReceipt(String trxHash) {
        JSONObject payload = new JSONObject()
                .fluentPut("jsonrpc", "2.0")
                .fluentPut("method", "eth_getTransactionReceipt")
                .fluentPut("id", "1")
                .fluentPut("params", new JSONArray().fluentAdd(trxHash));

        String response = HttpUtil.post(getJsonRpcURL(), payload.toString());
        JSONObject object = JSONObject.parseObject(response);

        return object != null ? object.getJSONArray("result") : null;
    }

    default Map<String, String> getTransactionByBlockNumber(String blockNumber, String transactionHash) {
        JSONObject payload = new JSONObject()
                .fluentPut("method", "eth_getBlockByNumber")
                .fluentPut("id", "1")
                .fluentPut("params", new Object[]{blockNumber.startsWith("0x") ? blockNumber : ("0x" + Long.toHexString(Long.parseLong(blockNumber))), true});

        JSONObject responseObject = JSON.parseObject(HttpUtil.post(getJsonRpcURL(), payload.toString()));
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

    default Map<String, String> getTransactionByHash(String transactionHash) {
        JSONObject payload = new JSONObject()
                .fluentPut("method", "eth_getTransactionByHash")
                .fluentPut("jsonrpc", "2.0")
                .fluentPut("id", "1")
                .fluentPut("params", new Object[]{transactionHash});

        JSONObject response = JSON.parseObject(HttpUtil.post(getJsonRpcURL(), payload.toString())).getJSONObject("result");

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

    default Map<String, String> getTransactionReceiptByHash(String transactionHash) {
        JSONObject payload = new JSONObject()
                .fluentPut("method", "eth_getTransactionReceipt")
                .fluentPut("jsonrpc", "2.0")
                .fluentPut("id", "1")
                .fluentPut("params", new Object[]{transactionHash});

        JSONObject response = JSON.parseObject(HttpUtil.post(getJsonRpcURL(), payload.toString())).getJSONObject("result");

        Map<String, String> result = new HashMap<>();
        String gasPrice = response.getString("gasUsed");
        long gasUsed = Long.parseLong(gasPrice.substring(2), 16);

        result.put("gasUsed", String.valueOf(gasUsed));
        String cumulativeGasUsed = response.getString("cumulativeGasUsed");
        result.put("cumulativeGasUsed", String.valueOf(Long.parseLong(cumulativeGasUsed.substring(2), 16)));

        return result;
    }
}
