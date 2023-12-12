package com.monitor.schedule.definition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("MonitorTotalSupplyJob")
public class MonitorTotalSupplyJob extends ScheduleJobDefinition {

    @Override
    protected void run() {
        this.start();

        log.info("monitor totalSupply");

        slackService.init();

        Map<String, BigDecimal> supplies = web3Service.queryTotalSupply();

        slackService.addTotalSupply(supplies.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString())));
        for (Map.Entry<String, BigDecimal> entry : supplies.entrySet()) {
            slackService.addMessage(entry.getKey(), Map.of("totalSupply", entry.getValue().toString()));
        }
        slackService.sendAsTable("tusd");

        this.end();
    }


}
