package com.monitor.schedule.definition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("CreateDailyReportJob")
@Slf4j
public class CreateDailyReportJob extends ScheduleJobDefinition {

    @Override
    protected void run() {
        dailyReportService.createDailyReport();
    }
}
