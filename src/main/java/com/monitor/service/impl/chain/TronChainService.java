package com.monitor.service.impl.chain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.service.parameter.QueryParam;
import com.monitor.utils.HttpUtil;
import com.monitor.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.monitor.constants.Topics.TOPIC;
import static com.monitor.service.parameter.QueryParam.optionalParam;
import static com.monitor.service.parameter.QueryParam.requiredParam;
import static com.monitor.utils.CommonUtil.GET_AMOUNT_VALUE;

@Slf4j
@Service("tronChainService")
public class TronChainService extends DefaultChainService {

    @Override
    public Integer getTransactionCount(QueryParam queryParam) {

        String url = getBlockchainScanURL(queryParam.getChain()) + "token_trc20/transfers?"
                + requiredParam("limit", "10000")
                + requiredParam("start", "10000")
                + requiredParam("contract_address", token.getContract(queryParam.getChain()))
                + optionalParam("start_timestamp", "0")
                + optionalParam("end_timestamp", "latest");

        JSONObject response = JSON.parseObject(HttpUtil.get(url));
        String count = response.getString("rangeTotal");
        log.info("count is :" + count);

        return Integer.parseInt(count);
    }

//    @Override
//    public Boolean getTransactionStatus(String hash) {
//
//        String url = getJsonRpcURL() + "walletsolidity/gettransactioninfobyid";
//
//        JSONObject payload = new JSONObject();
//        payload.put("value", hash);
//
//        String response = HttpUtil.post(url, payload.toString());
//        JSONObject obj = JSON.parseObject(response);
//
//        String result = obj.getJSONObject("receipt").getString("result");
//
//        return "SUCCESS".equals(result);
//    }
//
//    @Override
//    public JSONObject getAccountDetail(String address) {
//        String url = getJsonRpcURL() + "v1/accounts/" + address;
//
//        String response = HttpUtil.get(url);
//
//        return JSON.parseObject(response).getJSONArray("data").getJSONObject(0);
//    }

//    @Override
//    public BigDecimal getBalance(String address) {
//        String url = "https://apilist.tronscanapi.com/api/account/tokens?address=" + address + "&start=0&limit=20&hidden=0&show=0&sortType=0&sortBy=0&token=" + "TRX";
//
//        String result = HttpUtil.get(url);
//        if (result == null) {
//            return null;
//        }
//
//        JSONObject balanceResult = JSONObject.parseObject(result);
//        JSONArray balances = balanceResult.getJSONArray("data");
//
//        if (!CollectionUtils.isEmpty(balances)) {
//            JSONObject tUSDBalance = balances.getJSONObject(0);
//            if (tUSDBalance != null) {
//                return tUSDBalance.getBigDecimal("quantity");
//            }
//        }
//
//        return null;
//    }
//
//    @Override
//    public String getBlockByTimestamp(String timestamp) {
//        String requestUri = "https://apilist.tronscan.org/api/block?start_timestamp=" + timestamp +
//                requiredParam("end_timestamp", timestamp + 3000);
//
//        JSONObject response = JSON.parseObject(HttpUtil.get(requestUri));
//
//        JSONArray array = response.getJSONArray("data");
//        JSONObject obj = array.getJSONObject(0);
//        return obj.getString("number");
//    }

    /**
     * https://api.trongrid.io/v1/contracts/TUpMhErZL2fhh4sVNULAbNKLokS4GjC1F4/events?event_name=Mint
     * &order_by=block_timestamp,desc&min_block_timestamp=1659657600000&max_block_timestamp=
     *
     * @param queryParam
     * @return
     */
    @Override
    public Map<String, Map<String, String>> getTransactionsByEvent(QueryParam queryParam) {
        String requestUri = getJsonRpcURL(queryParam.getChain()) + "v1/contracts/" + queryParam.getAddress()
                + "/events?event_name=" + TOPIC(queryParam.getTopic())
                + optionalParam("min_block_timestamp", queryParam.getStartTime())
                + optionalParam("max_block_timestamp", queryParam.getEndTime())
                + "&order_by=block_timestamp,desc";

        log.info("response url={}", requestUri);

        JSONObject response = JSON.parseObject(HttpUtil.get(requestUri));

        Map<String, Map<String, String>> result = new HashMap<>();
        Map<String, String> transaction = convert(Objects.requireNonNull(response.getJSONArray("data")));
        result.put(transaction.get("transactionHash"), transaction);
        String url = response.getJSONObject("meta").getJSONObject("links").getString("next");
        while (url != null) {
            response = JSON.parseObject(HttpUtil.get(requestUri));
            transaction = convert(Objects.requireNonNull(response.getJSONArray("data")));
            result.put(transaction.get("transactionHash"), transaction);
            url = response.getJSONObject("meta").getJSONObject("links").getString("next");
        }
        return result;
    }

    private Map<String, String> convert(JSONArray results) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < results.size(); i++) {
            JSONObject result = results.getJSONObject(i);
            String blockNumber = result.getString("block_number");
            map.put("blockNumber", blockNumber);
            long timestamp = result.getLong("block_timestamp");
            map.put("timestamp", String.valueOf(timestamp));
            map.put("time", TimeUtil.FORMAT(new Date(timestamp), true));
            map.put("caller_contract_address", result.getString("caller_contract_address"));
            String trxId = result.getString("transaction_id");
            map.put("transactionHash", trxId);
            map.put("to", result.getJSONObject("result").getString("to"));
            map.put("burner", result.getJSONObject("result").getString("burner"));
            String value = result.getJSONObject("result").getString("value");
            BigDecimal amount = value == null ? new BigDecimal(0) : new BigDecimal(value);
            map.put("amount", GET_AMOUNT_VALUE(amount, "1000000000000000000", 2));
        }
        return map;
    }
}
