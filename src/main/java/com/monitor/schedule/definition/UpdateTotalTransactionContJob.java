package com.monitor.schedule.definition;

import com.monitor.service.interfaces.MarketSiteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("UpdateTotalTransactionContJob")
@Slf4j
public class UpdateTotalTransactionContJob extends ScheduleJobDefinition {

    @Autowired
    private MarketSiteService marketSiteService;

    @Override
    protected void run() {
        this.start();
        marketSiteService.updateTotalTransactionCount();
        this.end();
    }
}
