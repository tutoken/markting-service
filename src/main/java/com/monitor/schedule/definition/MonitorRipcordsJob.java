package com.monitor.schedule.definition;

import com.monitor.constants.Slack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("MonitorRipcordsJob")
public class MonitorRipcordsJob extends ScheduleJobDefinition {

    @Override
    public void run() {
        String status = dailyReportService.ripcords();
        if (status != null && !"".equals(status)) {
            slackService.sendNotice("tusd", String.format("```%s```", String.format("Current ripcords status is %s", status)));
            if ("Balances".equals(status)) {
                slackService.sendNotice("tusd", Slack.WARNING + slack.getID("Chichan") + slack.getID("Hosea"));
            } else {
                slackService.sendNotice("tusd", Slack.WARNING + slack.getID("Lily") + slack.getID("Hosea"));
            }
        }
    }
}
