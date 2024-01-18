package com.monitor.schedule.definition;

import com.monitor.constants.Slack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

import static com.monitor.utils.TimeUtil.getTime;

@Slf4j
@Service("MonitorRipcordsJob")
public class MonitorRipcordsJob extends ScheduleJobDefinition {

    private static final Date startTime = getTime(15, 0, 0);

    private static final Date endTime = getTime(16, 30, 0);

    @Override
    public void run() {
        String status = tokenService.ripcords();
        if (status != null && !"".equals(status)) {
            slackService.sendCodeBlockMessage("tusd",  String.format("Current ripcords status is %s", status));
            if (betweenPeriod()) {
                slackService.sendDirectMessage("tusd", Slack.WARNING + slack.getID("Chichan") + slack.getID("Lily"));
            }
        }
    }

    public boolean betweenPeriod() {
        Calendar current = Calendar.getInstance();
        Date currentTime = current.getTime();
        return currentTime.after(startTime) && currentTime.before(endTime);
    }
}
