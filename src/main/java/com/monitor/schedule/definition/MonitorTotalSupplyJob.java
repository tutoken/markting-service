package com.monitor.schedule.definition;

import com.monitor.constants.Monitor;
import com.monitor.service.parameter.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service("MonitorTotalSupplyJob")
public class MonitorTotalSupplyJob extends ScheduleJobDefinition {

    private static final String[] title = new String[]{"Chain", "totalSupply"};

    @Autowired
    private Monitor monitor;

    @Override
    protected void run() {
        log.info("monitor totalSupply");

        Message message = new Message();
        Map<String, BigDecimal> supplies = tokenService.queryTotalSupplyWithSummary();

        message.addCodeBlockMessage(String.format("Natively Networks TotalSupply: %s", supplies.get("Natively Networks TotalSupply")));
        message.addTable(title, monitor.getNativelyChains().toArray(String[]::new), supplies);
        message.addCodeBlockMessage(String.format("Bridged Networks TotalSupply: %s", supplies.get("Bridged Networks TotalSupply")));
        message.addTable(title, monitor.getBridgedChains().toArray(String[]::new), supplies);

        slackService.sendMessage("tusd", message);
    }
}
