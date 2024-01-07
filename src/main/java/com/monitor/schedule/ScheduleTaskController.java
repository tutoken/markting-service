package com.monitor.schedule;

import com.monitor.constants.Slack;
import com.monitor.schedule.definition.ScheduleJobDefinition;
import com.monitor.service.interfaces.SlackService;
import com.monitor.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ScheduleTaskController {

    @Autowired
    private ScheduleTaskGroup scheduleTaskGroup;

    @Scheduled(cron = "0 0 * * *  ?")
    public void task1() {
        scheduleTaskGroup.execute(List.of("MonitorLaunchPadJob"));
    }

    @Component
    static class ScheduleJobContext {
        @Autowired
        public final Map<String, ScheduleJobDefinition> taskDefinitionMap = new HashMap<>(2);

        public ScheduleJobDefinition taskOf(String name) {
            return taskDefinitionMap.get(name);
        }
    }

    @Component
    static class ScheduleTaskGroup {

        @Autowired
        private ScheduleJobContext scheduleJobContext;

        @Autowired
        private SlackService slackService;

        @Autowired
        private Slack slack;

        @Autowired
        private RedisUtil redisUtil;

        public void execute(List<String> jobs) {

            long lastTimeStamp = System.currentTimeMillis() - (1000 * 60 * 15);
            long currentTimeStamp = lastTimeStamp - (1000 * 60 * 60);

            log.info(String.format("Execute currentTimeStamp is %d, lastTimeStamp is %d", currentTimeStamp, lastTimeStamp));

            for (String jobName : jobs) {
                log.info(String.format("Start running %s", jobName));

                if (redisUtil.getBooleanValue(jobName)) {
                    log.error(jobName + " is running!");
                }
                try {
                    redisUtil.saveBooleanValue(jobName, true, 10, TimeUnit.MINUTES);
                    scheduleJobContext.taskOf(jobName).launch(String.valueOf(lastTimeStamp), String.valueOf(currentTimeStamp));
                } catch (Exception ex) {
                    log.error("run schedule task failed.", ex);

                    slackService.sendNotice("test", String.format("%s exception, lastTimeStamp: %d, currentTimeStamp: %d, cause: %s", jobName, lastTimeStamp, currentTimeStamp, ex));
                    slackService.sendNotice("test", ex.getMessage());
                    slackService.sendNotice("test", String.format("%s%s", Slack.WARNING, slack.getID("Hosea")));
                } finally {
                    redisUtil.deleteBooleanKey(jobName);
                }

                log.info(jobName + " end");
            }
        }
    }
}
