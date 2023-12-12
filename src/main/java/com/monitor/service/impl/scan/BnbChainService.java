package com.monitor.service.impl.scan;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.service.parameter.QueryParam;
import com.monitor.utils.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.monitor.service.parameter.QueryParam.optionalParam;
import static com.monitor.service.parameter.QueryParam.requiredParam;

@Slf4j
@Service("bnbChainService")
public class BnbChainService extends DefaultChainService {

    @Override
    public int getTransactionCount() {
        String countKey = chain + "_total_transaction_count";

        try {
            String url = getBlockchainScanURL()
                    + requiredParam("txAsset", "TUSDB-888")
                    + optionalParam("startTime", "0")
                    + optionalParam("endTime", String.valueOf(System.currentTimeMillis()))
                    + optionalParam("page", "1");

            JSONObject response = JSON.parseObject(HttpUtil.get(url));
            String count = response.getString("txNums");
            redisUtil.saveStringValue(countKey, count, 0, null);

            log.info("count is " + count);
            return Integer.parseInt(count);

        } catch (Exception ex) {
            return Integer.parseInt(redisUtil.getStringValueOrDefault(countKey, "0"));
        }
    }

    @Override
    public BigDecimal getBalance(String address) {
        String url = getJsonRpcURL() + "/account/" + address;
        JSONObject response = JSONObject.parseObject(HttpUtil.get(url));
        JSONArray balances = response.getJSONArray("balances");
        for (Object ob : balances.toArray()) {
            JSONObject balance = (JSONObject) ob;
            if ("BNB".equalsIgnoreCase(balance.getString("symbol"))) {
                return balance.getBigDecimal("free");
            }
        }

        return BigDecimal.ZERO;
    }

    @Override
    public JSONArray getERC20Transactions(QueryParam param) {
        String url = getBlockchainScanURL()
                + requiredParam("txAsset", "TUSDB-888")
                + requiredParam("txType", param.getTopic())
                + optionalParam("startTime", param.getStartTime())
                + optionalParam("endTime", param.getEndTime())
                + requiredParam("page", param.getPage());

        JSONObject response = JSON.parseObject(HttpUtil.get(url));
        return response.getJSONArray("txArray");
    }
}
