package com.monitor.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface MarketSiteService {

    /**
     * Get the current currency price（for schedule job）
     *
     * @param symbol
     * @param convert
     * @return
     */
    String currentPrice(String symbol, String convert);

    /**
     * Get overview
     */
    Map<String, String> overview();

    /**
     * Get ecosystem information
     */
    Map<String, Map<String, Map<String, String>>> ecosystem();

    /**
     * Update erc20 transactions count
     */
    void updateTotalTransactionCount();

    /**
     * Upload file to server
     */
    @Deprecated
    void uploadFile(MultipartFile file);
}


