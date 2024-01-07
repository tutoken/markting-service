package com.monitor.schedule.definition;

import com.monitor.constants.Slack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.monitor.constants.Monitor.DECIMAL18;
import static com.monitor.utils.CommonUtil.FORMAT;

@Service("MonitorBalanceJob")
@Slf4j
public class MonitorBalanceJob extends ScheduleJobDefinition {

    @Override
    protected void run() {
        slackService.init();

        for (String chain : monitor.getNativelyChains()) {
            List<String> addresses = monitor.getAddress(chain);
            if (CollectionUtils.isEmpty(addresses)) {
                continue;
            }
            for (String address : addresses) {
                BigDecimal balance = tokenService.getBalance(chain, address);
                if (balance.compareTo(new BigDecimal(monitor.getBalanceThreshold(chain))) < 0) {
                    Map<String, String> message = new HashMap<>();
                    message.put("Balance", balance.divide(DECIMAL18, 18, RoundingMode.HALF_UP).toString());
                    message.put("Chain", chain);
                    slackService.addMessage(address, message);
                    slackService.addWarning(String.format("Balance of %s is too low: %s", slack.getLink(chain, address, address), FORMAT(balance.toString())));
                    slackService.addWarning(Slack.WARNING + slack.getID("Tahoe") + slack.getID("Lily") + slack.getID("Hosea"));
                }
            }
        }

        slackService.sendAsTable("tusd");
    }
}
