package com.monitor.service.interfaces.chain;

import com.alibaba.fastjson.JSONObject;
import com.monitor.utils.HttpUtil;

import java.math.BigDecimal;
import java.util.Objects;

public interface ChainScanService extends BlockchainService {

    /**
     * get api key
     *
     * @return
     */
    String getApiKey(String chain);

    /**
     * get url for blockchain scanner
     *
     * @return
     */
    String getBlockchainScanURL(String chain);

    default BigDecimal getBalance(String chain, String address) {
        Objects.requireNonNull(address, "account address can not be null.");

        String url = getBlockchainScanURL(chain) + "module=account&action=balance&address="
                + address + "&apikey=" + getApiKey(chain);

        String result = HttpUtil.get(url);

        if (result == null) {
            return null;
        }
        JSONObject balanceResult = JSONObject.parseObject(result);
        return balanceResult == null ? null : balanceResult.getBigDecimal("result");
    }
}

