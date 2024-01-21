package com.monitor.schedule.definition;

import com.monitor.constants.Slack;
import com.monitor.schedule.base.ScheduleJobDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static com.monitor.constants.Monitor.TUSD_PRICE;


@Service("MonitorPriceJob")
@Slf4j
public class MonitorPriceJob extends ScheduleJobDefinition {
    @Override
    public void run() {
        String price = tokenService.currentPrice("TUSD", "USD");
        if (price != null && !"".equals(price)) {
            BigDecimal currentPrice = new BigDecimal(price);
            if (currentPrice.compareTo(TUSD_PRICE) < 0) {
                slackService.sendCodeBlockMessage("tusd", String.format("Current price is $%s", price));
                slackService.sendWarning("tusd", "Peggy", "Teresa", "Maria");
            }
        }
    }
}
