package com.monitor.schedule.definition;

import com.monitor.schedule.base.ScheduleJobDefinition;
import com.monitor.service.parameter.Message;
import com.monitor.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;


@Service("MonitorPriceJob")
@Slf4j
public class MonitorPriceJob extends ScheduleJobDefinition {

    @Autowired
    private RedisUtil redisUtil;

    private static final String FOUR_HOURS_KEY = "MONITOR_PRICE_JOB_FOUR_HOURS";

    private static final String TWELVE_HOURS_KEY = "MONITOR_PRICE_JOB_TWELVE_HOURS";

    private static final BigDecimal PRICE_LEVEL_1 = new BigDecimal("0.99");

    private static final BigDecimal PRICE_LEVEL_2 = new BigDecimal("0.98");

    /**
     * No alarm will be issued if the price is above 0.99
     * Every 12 hours when the price is between 0.98 and 0.99
     * Every 4 hours when the price is below 0.98
     */
    @Override
    public void run() {
        String price = tokenService.currentPrice("TUSD", "USD");
        log.info("price is " + price);

        if (price != null && !"".equals(price)) {
            BigDecimal currentPrice = new BigDecimal(price);

            if (currentPrice.compareTo(PRICE_LEVEL_1) < 0) {
                if (currentPrice.compareTo(PRICE_LEVEL_2) > 0 && !redisUtil.getBooleanValue(FOUR_HOURS_KEY)) {
                    Message message = new Message();
                    message.addCodeBlockMessage(String.format("```%s```", String.format("Current price is $%s", price)));
                    this.sendMessage(message);
                    this.noticeRecipients();
                    redisUtil.saveBooleanValue(FOUR_HOURS_KEY, true, 4, TimeUnit.HOURS);

                } else if (currentPrice.compareTo(PRICE_LEVEL_2) < 0 && !redisUtil.getBooleanValue(TWELVE_HOURS_KEY)) {
                    Message message = new Message();
                    message.addCodeBlockMessage(String.format("```%s```", String.format("Current price is $%s", price)));
                    this.sendMessage(message);
                    this.noticeRecipients();
                    redisUtil.saveBooleanValue(TWELVE_HOURS_KEY, true, 12, TimeUnit.HOURS);
                }
            }
        }
    }
}
