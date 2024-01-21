package com.monitor.schedule.definition;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.constants.Slack;
import com.monitor.schedule.base.ScheduleJobDefinition;
import com.monitor.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("MonitorLaunchPadJob")
public class MonitorLaunchPadJob extends ScheduleJobDefinition {

    private static final Long PERIOD = 24 * 60 * 60 * 1000L;

    private static final String BASE_URL = "https://launchpad.binance.com/en/launchpool/";

    @Override
    public void run() {
        JSONArray trackingList = tokenService.getLaunchPool();

        for (Object launchPool : trackingList) {
            JSONObject tracking = (JSONObject) launchPool;
            JSONArray projects = tracking.getJSONArray("projects");

            if (projects == null) {
                return;
            }

            String rebateCoin = tracking.getString("rebateCoin");
            String status = tracking.getString("status");
            String detailAbstract = tracking.getString("detailAbstract");
            long mineEndTime = tracking.getLong("mineEndTime");

            long period = ChronoUnit.MILLIS.between(Instant.now(), Instant.ofEpochMilli(mineEndTime));

            log.warn("Period is " + period);

            String key = rebateCoin + "_" + mineEndTime;
            String value = redisUtil.getStringValueOrDefault(key, null);

            if ("WARNED".equals(value)) {
                continue;
            }

            StringBuilder message = new StringBuilder();
            for (Object object : projects) {
                JSONObject project = (JSONObject) object;
                if ("TUSD".equalsIgnoreCase(project.getString("asset"))) {
                    String projectId = project.getString("projectId");
                    message.append(String.format("%s\n", BASE_URL + projectId));
                }
            }

            if (!"".equals(message.toString())) {
                if (status != null && !status.equals(value)) {
                    slackService.sendCodeBlockMessage("tusd", String.format("%s (%s)\nStatus: %s\nTime until farming ends: %s", rebateCoin, detailAbstract, status, TimeUtil.MONITOR(period)));
                    slackService.sendCodeBlockMessage("tusd", message.toString());
                    slackService.sendWarning("tusd", "Lily", "Tahoe");
                    redisUtil.saveStringValue(key, status, 0, null);
                }

                if (period <= PERIOD && period > 0) {
                    slackService.sendCodeBlockMessage("tusd", String.format("%s (%s)\nTime until farming ends: %s", rebateCoin, detailAbstract, TimeUtil.MONITOR(period)));
                    slackService.sendCodeBlockMessage("tusd", message.toString());
                    slackService.sendWarning("tusd", "Lily", "Tahoe");

                    redisUtil.saveStringValue(key, "WARNED", 25, TimeUnit.HOURS);
                }
            }
        }
    }
}
