package com.monitor.service.interfaces;

import com.monitor.service.parameter.QueryParam;
import com.monitor.service.parameter.TotalSupply;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * All externally provided interfaces
 */
public interface Web3Service {

    /**
     * Get all transactions within the specified block range at the current address
     *
     * @param queryParam
     * @return
     */
    Set<Map<String, String>> getTransactionsByAddress(QueryParam queryParam);

    /**
     * Get a list of transactions of the specified type
     *
     * @param queryParam
     * @return
     */
    Map<String, Map<String, String>> getSpecifiedTransactions(QueryParam queryParam);

    /**
     * Get the balance of the current address
     *
     * @param chain
     * @param address
     * @return
     */
    BigDecimal getBalance(String chain, String address);

    /**
     * Get the total supply at the current moment
     *
     * @return
     */
    // TODO separated with comma
    List<TotalSupply> getTotalSupply();

    Map<String, BigDecimal> queryTotalSupplyWithSummary();

    /**
     * Get the total supplly at the current moment (for schedule job)
     *
     * @return
     */
    Map<String, BigDecimal> queryTotalSupply();

    /**
     * Reverse the total supply at the specified time from the reference time
     *
     * @param chain
     * @param timestamp
     * @return
     */
    Map<String, BigDecimal> queryTotalSupplyByTimestamp(String chain, String timestamp);


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
}
