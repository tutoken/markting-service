package com.monitor.service.interfaces;

import com.alibaba.fastjson.JSONArray;
import com.monitor.service.parameter.QueryParam;
import com.monitor.service.parameter.TotalSupply;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * All externally provided interfaces
 */
public interface TokenService {
    /**
     * Get the current currency price（for schedule job）
     *
     * @param symbol
     * @param convert
     * @return
     */
    String currentPrice(String symbol, String convert);

    /**
     * Get the total supply at the current moment
     *
     * @return
     */
    List<TotalSupply> getTotalSupply();

    Map<String, BigDecimal> queryTotalSupplyByTimestamp(String chain, String time);

    Map<String, BigDecimal> queryTotalSupplyWithSummary();

    /**
     * Get a list of transactions of the specified type
     *
     * @param queryParam
     * @return
     */
    Map<String, Map<String, String>> getSpecifiedTransactions(QueryParam queryParam, Function<Object, Boolean> condition);


    /**
     * Get a list of TUSD mint transactions
     *
     * @param queryParam
     * @return
     */
    Map<String, Map<String, String>> getMintTransactions(QueryParam queryParam);

    /**
     * Get a list of TUSD redemption transactions
     *
     * @param queryParam
     * @return
     */
    Map<String, Map<String, String>> getRedemptionTransactions(QueryParam queryParam);

    /**
     * Get the balance of the current address
     *
     * @param chain
     * @param address
     * @return
     */
    BigDecimal getBalance(String chain, String address);

    String ripcords();

    JSONArray getLaunchPool();
}
