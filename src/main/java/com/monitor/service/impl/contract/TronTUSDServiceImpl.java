package com.monitor.service.impl.contract;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.service.interfaces.TUSDService;
import com.monitor.utils.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

@Service("tronTUSDService")
public class TronTUSDServiceImpl extends TUSDService {
    private static final Map<String, String> proxyOwners = new HashMap<>();

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public String queryContract(String chain, String field) {
        return this.queryTron(token.getContract("tron"), field);
    }

    @Override
    public String funcContract(String chain, String funcName, Map<Class, Object> fields) {
        return null;
    }

    @Override
    public String queryController(String chain, String field) {
        String tronProxyOwner = proxyOwners.getOrDefault(chain, this.getProxyOwner(chain));
        return this.queryTron(tronProxyOwner, field);
    }

    private String getProxyOwner(String chain) {
        if (proxyOwners.containsKey(chain)) {
            return proxyOwners.get(chain);
        }
        String proxyOwner = this.queryTron(token.getContract("tron"), "owner");
        logger.info(String.format("proxyOwner: %s", proxyOwner));
        proxyOwner = "41" + proxyOwner.substring(proxyOwner.length() - 40);
        return proxyOwners.put(chain, proxyOwner);
    }

    private String queryTron(String contractAddress, String functionSelector) {
        String url = web3Provider.getRpcUrl("tron");
        JSONObject payload = new JSONObject();
        payload.put("contract_address", contractAddress);
        payload.put("owner_address", token.getOwner("tron"));
        payload.put("function_selector", functionSelector + "()");
        payload.put("visible", true);

        String response = HttpUtil.post(url, payload.toString());
        JSONObject obj = JSON.parseObject(response);

        if (obj == null) {
            return "";
        }

        JSONArray results = obj.getJSONArray("constant_result");

        if (CollectionUtils.isEmpty(results)) {
            return "";
        }

        return results.getString(0);
    }
}
