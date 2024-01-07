package com.monitor.service.impl.chain;

import com.alibaba.fastjson.JSONObject;
import com.monitor.service.parameter.QueryParam;
import com.monitor.utils.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("cronosChainService")
public class CronosChainService extends DefaultChainService {

    @Override
    public Integer getTransactionCount(QueryParam queryParam) {
        String url = "https://cronos.org/explorer/token-counters?id=" + token.getContract(queryParam.getChain());

        JSONObject response = JSONObject.parseObject(HttpUtil.get(url));
        String count = response.getString("transfer_count");

        return Integer.parseInt(count);
    }
}
