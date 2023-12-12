package com.monitor.service.interfaces.chain;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.service.parameter.QueryParam;

import java.math.BigDecimal;
import java.util.Map;

public interface BlockchainService {

    /**
     * init blockchain id
     *
     * @param chain
     * @return
     */
    BlockchainService init(String chain);

    /**
     * get label of an address
     *
     * @param address
     * @return
     */
    String getLabel(String address);

    /**
     * Query transactions by address from blockchain scanner
     *
     * @param
     * @return
     */
    JSONArray getTransactionsByAddress(QueryParam param);

    /**
     * Obtain account details
     *
     * @param address
     * @return
     */
    JSONObject getAccountDetail(String address);

    /**
     * Get transaction status
     *
     * @param hash
     * @return
     */
    Boolean getTransactionStatus(String hash);

    /**
     * Get block number by timestamp
     *
     * @param timestamp
     * @return
     */
    String getBlockByTimestamp(String timestamp);

    /**
     * Get balance of a specified address
     *
     * @param address
     * @return
     */
    BigDecimal getBalance(String address);

    /**
     * get ERC20 transaction of an address
     *
     * @param param
     * @return
     */
    JSONArray getERC20Transactions(QueryParam param);

    /**
     * Scan by event
     *
     * @param queryParam
     * @return
     */
    Map<String, Map<String, String>> getTransactionsByEvent(QueryParam queryParam);

    /**
     * Obtain the receipt of a transaction
     *
     * @param trxHash
     * @return
     */
    JSONArray getTransactionReceipt(String trxHash);

    /**
     * Block by block scanning
     *
     * @param blockNumber
     * @return
     */
    // TODO return value
    Map<String, String> getTransactionByBlockNumber(String blockNumber, String transactionHash);

    /**
     * Get transaction by hash
     *
     * @param trx_hash
     * @return
     */
    Map<String, String> getTransactionByHash(String trx_hash);

    /**
     * Get transaction receipt by hash
     *
     * @param transactionHash
     * @return
     */
    Map<String, String> getTransactionReceiptByHash(String transactionHash);

    /**
     * Get total transactions count
     */
    int getTransactionCount();
}
