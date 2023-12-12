package com.monitor.service.impl.contract;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.service.interfaces.TUSDService;
import com.monitor.utils.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service("bnbTUSDService")
public class BNBTUSDServiceImpl extends TUSDService {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    private static final String TUSD = "TUSDB-888";

    @Override
    public String queryContract(String chain, String field) {
        try {
            return "totalSupply".equals(field) ? this.queryBnbTotalSupply(web3Provider.getRpcUrl(chain)).setScale(2, RoundingMode.UP).toString() : "";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "N/A";
        }
    }

    @Override
    public String funcContract(String chain, String funcName, Map<Class, Object> fields) {
        return null;
    }

    @Override
    public String queryController(String chain, String field) {
        return "";
    }

    private BigDecimal queryBnbTotalSupply(String url) throws Exception {
        Map<String, String> header = Map.of("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36");
        List<String> bnbAddresses = List.of(token.getLock("bnb"), token.getOwner("bnb"));
        BigDecimal sum = new BigDecimal(0);
        for (String address : bnbAddresses) {
            JSONObject obj = JSON.parseObject(HttpUtil.get(url + "/account/" + address, header));
            if (obj == null) {
                continue;
            }
            JSONArray balances = obj.getJSONArray("balances");
            for (Object ob : balances.toArray()) {
                JSONObject balance = (JSONObject) ob;
                if (TUSD.equals(balance.getString("symbol"))) {
                    sum = sum.add(balance.getBigDecimal("free").add(balance.getBigDecimal("frozen")).add(balance.getBigDecimal("locked")));
                    break;
                }
            }
        }
        JSONArray tokens = JSON.parseArray(HttpUtil.get(url + "/tokens?limit=1000"));
        if (tokens == null) {
            return new BigDecimal("90000000000").subtract(sum);
        }
        for (Object obj : tokens) {
            JSONObject token = (JSONObject) obj;
            if (TUSD.equals(token.getString("symbol"))) {
                return token.getBigDecimal("total_supply").subtract(sum);
            }
        }

        return new BigDecimal("90000000000").subtract(sum);
    }
}
