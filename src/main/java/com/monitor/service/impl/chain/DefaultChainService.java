package com.monitor.service.impl.chain;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.constants.Web3Provider;
import com.monitor.service.interfaces.chain.ChainJsonRpcService;
import com.monitor.service.interfaces.chain.ChainScanService;
import com.monitor.service.parameter.QueryParam;
import com.monitor.utils.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static com.monitor.constants.Token.TUSD;
import static com.monitor.constants.Topics.TOPIC;
import static com.monitor.utils.CommonUtil.GET_AMOUNT_VALUE;
import static org.web3j.utils.Numeric.toHexStringWithPrefix;

@Slf4j
@Scope("prototype")
@Service("defaultChainService")
public abstract class DefaultChainService implements ChainJsonRpcService, ChainScanService {

    @Autowired
    protected Web3Provider web3Provider;

    @Autowired
    protected TUSD token;

    @Override
    public String getJsonRpcURL(String chain) {
        return this.web3Provider.getRpcUrl(chain);
    }

    @Override
    public String getApiKey(String chain) {
        return this.web3Provider.getScanKey(chain);
    }

    @Override
    public String getBlockchainScanURL(String chain) {
        return this.web3Provider.getScanUrl(chain);
    }

    @Override
    public Map<String, Map<String, String>> getTransactionsByEvent(QueryParam queryParam) {
        JSONObject payload = new JSONObject()
                .fluentPut("jsonrpc", "2.0")
                .fluentPut("method", queryParam.getMethod())
                .fluentPut("id", "1");

        JSONObject param = new JSONObject()
                .fluentPut("address", queryParam.getAddress())
                .fluentPut("topics", TOPIC(queryParam.getTopic()))
                .fluentPut("fromBlock", toHexStringWithPrefix(new BigInteger(queryParam.getStartBlock())))
                .fluentPut("toBlock", toHexStringWithPrefix(new BigInteger(queryParam.getEndBlock())));

        JSONArray params = new JSONArray().fluentAdd(param);
        payload.put("params", params);

        String response = HttpUtil.post(getJsonRpcURL(queryParam.getChain()), payload.toString());
        JSONObject object = JSONObject.parseObject(response);

        if (object == null) {
            return null;
        }

        JSONArray transactions = object.getJSONArray("result");
        if (CollectionUtils.isEmpty(transactions)) {
            return null;
        }

        Map<String, Map<String, String>> result = new HashMap<>();
        for (int i = 0; i < transactions.size(); i++) {
            JSONObject transaction = transactions.getJSONObject(i);
            Map<String, String> info = new HashMap<>();

            String blockNumber = transaction.getString("blockNumber");
            int blockNum = Integer.parseInt(blockNumber.substring(2), 16);

            String trx_hash = transaction.getString("transactionHash");

            info.put("blockNumber", String.valueOf(blockNum));
            info.put("transactionHash", trx_hash);
            info.put("blockHash", transaction.getString("blockHash"));

            JSONArray topics = transaction.getJSONArray("topics");
            Object toAddress = topics.get(1);
            String to = toAddress == null ? "" : "0x" + toAddress.toString().substring(26);
            info.put("to", to);

            String data = (transaction.getString("data"));
            BigDecimal amount = data == null ? BigDecimal.ZERO : new BigDecimal(Numeric.toBigInt(transaction.getString("data")));
            info.put("amount", GET_AMOUNT_VALUE(amount, "1000000000000000000", 2));

            BigDecimal gasUsedValue = new BigDecimal(info.get("gasPrice")).multiply(new BigDecimal(info.get("gasUsed")));
            info.put("gasUsedValue", String.valueOf(gasUsedValue));

            result.put(trx_hash, info);
        }
        return result;
    }
}
