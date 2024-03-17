package com.monitor.schedule.definition;

import com.monitor.schedule.base.ScheduleJobDefinition;
import com.monitor.service.parameter.Message;
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

    private static final String[] title = new String[]{"Address", "Chain", "Balance"};

    @Override
    protected void run() {
        Message message = new Message();
        Map<String, Map<String, String>> waringTable = new HashMap<>();

        for (String chain : monitor.getNativelyChains()) {
            List<String> addresses = monitor.getAddress(chain);
            if (CollectionUtils.isEmpty(addresses)) {
                continue;
            }

            for (String address : addresses) {
                BigDecimal balance = tokenService.getBalance(chain, address);
                if (balance.compareTo(new BigDecimal(monitor.getBalanceThreshold(chain))) < 0) {
                    message.addDirectMessage(String.format("Balance of %s is too low: %s", slack.getLink(chain, address, address), FORMAT(balance.toString())));
                    Map<String, String> balanceMap = Map.of("Chain", chain, "Balance", balance.divide(DECIMAL18, 18, RoundingMode.HALF_UP).toString());
                    waringTable.put(address, balanceMap);
                }
            }
        }


        if (!waringTable.isEmpty()) {
            message.addTable(title, waringTable);
            slackService.sendMessage(getDefaultChannel(), message);
            this.noticeRecipients();
        }
    }
}
