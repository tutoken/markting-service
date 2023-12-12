package com.monitor.service.impl.scan;

import com.alibaba.fastjson.JSONObject;
import com.monitor.utils.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Deprecated
@Slf4j
@Service("hecoChainService")
public class HecoChainService extends DefaultChainService {

    public int getTransactionCount() {
        String lastCountKey = chain + "_total_transaction_last_count";
        String countKey = chain + "_total_transaction_count";

        String url = "https://www.hecoinfo.com/api/v1/chain/address/overview?a="
                + token.getContract(chain)
                + "&chainId=HECO&type=2";
        String count = redisUtil.getStringValueOrDefault(lastCountKey, "0");
        if (!"0".equals(count)) {
            return Integer.parseInt(count);
        }
        try {
            JSONObject response = JSONObject.parseObject(HttpUtil.get(url));
            count = response.getJSONObject("data").getString("transfers");
        } catch (Exception e) {
            return Integer.parseInt(redisUtil.getStringValueOrDefault(countKey, "0"));
        }

        redisUtil.saveStringValue(lastCountKey, count, 15, TimeUnit.MINUTES);
        redisUtil.saveStringValue(count, count, 0, null);

        return Integer.parseInt(count);
    }
}
