package com.monitor.service.impl.chain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.monitor.service.parameter.QueryParam;
import com.monitor.utils.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.monitor.service.parameter.QueryParam.optionalParam;
import static com.monitor.service.parameter.QueryParam.requiredParam;

@Slf4j
@Service("bnbChainService")
public class BnbChainService extends DefaultChainService {

    @Override
    public Integer getTransactionCount(QueryParam queryParam) {
        String url = getBlockchainScanURL(queryParam.getChain())
                + requiredParam("txAsset", "TUSDB-888")
                + optionalParam("startTime", "0")
                + optionalParam("endTime", String.valueOf(System.currentTimeMillis()))
                + optionalParam("page", "1");

        JSONObject response = JSON.parseObject(HttpUtil.get(url));
        String count = response.getString("txNums");

        log.info("count is " + count);
        return Integer.parseInt(count);
    }

    @Override
    public Map<String, Map<String, String>> getTransactionsByEvent(QueryParam queryParam) {
        return null;
    }
}
