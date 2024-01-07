package com.monitor.service.interfaces.chain;

import com.monitor.service.parameter.QueryParam;

import java.math.BigDecimal;
import java.util.Map;

public interface BlockchainService {

    /**
     * Scan by event
     *
     * @param queryParam
     * @return
     */
    Map<String, Map<String, String>> getTransactionsByEvent(QueryParam queryParam);

    /**
     * Block by block scanning
     *
     * @param blockNumber
     * @return
     */
    // TODO return value
    Map<String, String> getTransactionByBlockNumber(String chain, String blockNumber, String transactionHash);

    /**
     * Get transaction by hash
     *
     * @param trx_hash
     * @return
     */
    Map<String, String> getTransactionByHash(String chain, String trx_hash);

    /**
     * Get transaction receipt by hash
     *
     * @param transactionHash
     * @return
     */
    Map<String, String> getTransactionReceiptByHash(String chain, String transactionHash);

    /**
     * Get total transactions count
     */
    Integer getTransactionCount(QueryParam queryParam);

    /**
     * Get latest block number
     *
     * @param chain
     * @return
     */
    String getLatestBlockNumber(String chain);

    BigDecimal getBalance(String chain, String address);
}
