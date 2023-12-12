package com.monitor.service.impl.scan;

import com.alibaba.fastjson.JSONObject;
import com.monitor.utils.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("cronosChainService")
public class CronosChainService extends DefaultChainService {

    public int getTransactionCount() {
        String countKey = chain + "_total_transaction_count";
        String url = "https://cronos.org/explorer/token-counters?id=" + token.getContract(chain);

        try {
            JSONObject response = JSONObject.parseObject(HttpUtil.get(url));
            String count = response.getString("transfer_count");

            redisUtil.saveStringValue(countKey, count, 0, null);

            return Integer.parseInt(count);
        } catch (Exception e) {
            return Integer.parseInt(redisUtil.getStringValueOrDefault(countKey, "0"));
        }
    }
}
