package com.monitor.service.interfaces;

import java.util.Map;

public interface DailyReportService {

    /**
     * Get real-time reserve from cache or database
     */
    Map<String, Object> getDailyReport();

    /**
     * Get and store real-time reserve from LedgerLens
     */
    void createDailyReport();

    /**
     * Download report from LedgerLens and upload it to S3
     */
    String restoreReport();

    /**
     * Get the current alert
     *
     * @return
     */
    String ripcords();
}
