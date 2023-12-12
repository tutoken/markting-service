package com.monitor.service.interfaces.chain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.service.parameter.QueryParam;
import com.monitor.utils.HttpUtil;
import org.jsoup.Jsoup;
import org.springframework.util.Assert;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;

import static com.monitor.service.parameter.QueryParam.optionalParam;
import static com.monitor.service.parameter.QueryParam.requiredParam;


public interface ChainScanService extends BlockchainService {

    /**
     * get api key
     *
     * @return
     */
    String getApiKey();

    /**
     * get url for blockchain scanner
     *
     * @return
     */
    String getBlockchainScanURL();

    default String getLabel(String address) {
        String uri = "https://etherscan.io/address/" + address;
        String title;
        try {
            title = Jsoup.connect(uri).get().title();
        } catch (IOException e) {
            title = address;
        }
        if (!title.contains("| Address " + address + " |")) {
            return address;
        }

        title = title.substring(0, title.indexOf("| Address " + address + " |") - 1);
        return "".equals(title) ? address : title;
    }

    default JSONArray getTransactionsByAddress(QueryParam param) {
        Assert.notNull(param.getAddress(), "address can not be null");

        String response = HttpUtil.get(getBlockchainScanURL() + "module=account" +
                requiredParam("action", "txlist") +
                requiredParam("address", param.getAddress()) +
                requiredParam("startblock", param.getStartBlock()) +
                requiredParam("endblock", param.getEndBlock()) +
                optionalParam("page", param.getPage()) +
                optionalParam("offset", param.getOffset()) +
                optionalParam("apikey", getApiKey()));

        JSONObject obj = JSON.parseObject(response);

        return obj.getJSONArray("result");
    }

    default JSONObject getAccountDetail(String address) {
        return null;
    }

    default Boolean getTransactionStatus(String hash) {
        Objects.requireNonNull(hash, "transaction hash can not be null");

        // max rate 5 request per second
        try {
            Thread.sleep(1000 / 5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String url = getBlockchainScanURL() + "module=transaction&action=getstatus&txhash=" + hash +
                requiredParam("apikey", getApiKey());

        String response = HttpUtil.get(url);
        JSONObject status = JSONObject.parseObject(response).getJSONObject("result");

        return status != null && status.getInteger("isError") == 0;
    }

    default String getBlockByTimestamp(String timestamp) {
        Objects.requireNonNull(timestamp, "timestamp can not be null.");
        long time = Long.parseLong(timestamp) / 1000;

        String requestUri = getBlockchainScanURL() + "action=getblocknobytime&module=block&timestamp=" + time +
                "&closest=before" + optionalParam("apikey", getApiKey());

        JSONObject response = JSON.parseObject(HttpUtil.get(requestUri));
        return response.getString("result");
    }

    default BigDecimal getBalance(String address) {
        Objects.requireNonNull(address, "account address can not be null.");

        String url = getBlockchainScanURL() + "module=account&action=balance&address=" + address +
                "&apikey=" + getApiKey();

        String result = HttpUtil.get(url);

        if (result == null) {
            return null;
        }
        JSONObject balanceResult = JSONObject.parseObject(result);
        return balanceResult == null ? null : balanceResult.getBigDecimal("result");
    }

    default JSONArray getERC20Transactions(QueryParam param) {
        Assert.notNull(param.getEndBlock(), "address can not be null");
        String url = getBlockchainScanURL() + "module=account" +
                requiredParam("action", "tokentx") +
                optionalParam("address", param.getAddress()) +
                requiredParam("contractaddress", param.getContractAddress()) +
                optionalParam("startblock", param.getStartBlock()) +
                optionalParam("endblock", param.getEndBlock()) +
                optionalParam("limit", param.getLimit()) +
                optionalParam("offset", param.getOffset()) +
                optionalParam("page", param.getPage()) +
                optionalParam("apikey", getApiKey());

        JSONObject response = JSON.parseObject(HttpUtil.get(url));
        return response == null ? null : response.getJSONArray("result");
    }
}
