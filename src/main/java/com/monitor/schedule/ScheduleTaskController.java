package com.monitor.schedule;

import com.monitor.constants.Slack;
import com.monitor.schedule.definition.ScheduleJobDefinition;
import com.monitor.service.interfaces.SlackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ScheduleTaskController {

    @Autowired
    private ScheduleTaskGroup scheduleTaskGroup;

    @Scheduled(cron = "0 0 * * *  ?")
    public void task1() {
        scheduleTaskGroup.execute(List.of("UpdateTotalTransactionContJob"));
    }

    @Scheduled(cron = "0 30 8 * * ?")
    public void task2() {
        scheduleTaskGroup.execute(List.of("CreateDailyReportJob"));
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

        public void execute(List<String> jobs) {

            long lastTimeStamp = System.currentTimeMillis() - (1000 * 60 * 15);
            long currentTimeStamp = lastTimeStamp - (1000 * 60 * 60);

            log.info(String.format("Execute currentTimeStamp is %d, lastTimeStamp is %d", currentTimeStamp, lastTimeStamp));

            for (String jobName : jobs) {
                log.info(String.format("Start running %s", jobName));

                try {
                    scheduleJobContext.taskOf(jobName).launch(String.valueOf(lastTimeStamp), String.valueOf(currentTimeStamp));
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);

                    slackService.sendNotice("test", String.format("%s exception, lastTimeStamp: %d, currentTimeStamp: %d", jobName, lastTimeStamp, currentTimeStamp));
                    slackService.sendNotice("test", ex.getMessage());
                    slackService.sendNotice("test", String.format("%s%s", Slack.WARNING, slack.getID("Hosea")));
                }

                log.info(jobName + " end");
            }
        }
    }
}
