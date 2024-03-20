package com.monitor.schedule.base;

import com.monitor.database.model.SchedulerJobDetail;
import com.monitor.database.repository.SchedulerJobDetailRepository;
import com.monitor.service.interfaces.SlackService;
import com.monitor.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ScheduleTaskExecutor implements Job {

    @Autowired
    public final Map<String, ScheduleJobDefinition> serviceContext = new HashMap<>();

    @Autowired
    private SlackService slackService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private SchedulerJobDetailRepository schedulerJobDetailRepository;

    @Override
    public void execute(JobExecutionContext context) {
        long groupId = Long.parseLong(context.getJobDetail().getKey().getName());

        List<SchedulerJobDetail> schedulerJobDetails = schedulerJobDetailRepository.findByGroupIdOrderByPriority(groupId);

        if (CollectionUtils.isEmpty(schedulerJobDetails)) {
            slackService.sendDirectMessage("test", String.format("No jobs in %d", groupId));
            slackService.sendWarning("test", "Hosea");
            return;
        }

        this.execute(schedulerJobDetails.stream().filter(schedulerJobDetail -> {
            String definition = schedulerJobDetail.getDefinition();
            if (schedulerJobDetail.isEnable()) {
                return true;
            }
            log.warn(String.format("%s is skipped.", definition));
            slackService.sendDirectMessage("test", String.format("%s is skipped.", definition));
            slackService.sendWarning("test", "Hosea");
            return false;

        }).collect(Collectors.toList()));
    }

    public void execute(List<SchedulerJobDetail> schedulerJobDetails) {
        long currentTime = System.currentTimeMillis();
        schedulerJobDetails.forEach(schedulerJobDetail -> {
            if (schedulerJobDetail == null) {
                return;
            }
            String definition = schedulerJobDetail.getDefinition();
            try {
                log.info(String.format("Start running %s, current time is %d", definition, currentTime));
                if (!lockJob(definition, schedulerJobDetail.getTimeoutDuration())) {
                    return;
                }
                ScheduleJobDefinition jobDefinition = serviceContext.get(definition);
                if (jobDefinition == null) {
                    throw new RuntimeException(String.format("Can not find job %s definition", definition));
                }
                jobDefinition.execute(currentTime, schedulerJobDetail);
            } catch (Exception ex) {
                log.error("run schedule task failed.", ex);

                slackService.sendDirectMessage("test", String.format("%s exception, current time : %d, cause: %s", definition, currentTime, ex));
                slackService.sendWarning("test", "Hosea");
            } finally {
                this.unlockJob(definition);
                log.info(definition + " end");
            }
        });
    }

    private boolean lockJob(String definition, int timeoutDuration) {
        if (redisUtil.getBooleanValue(definition)) {
            log.error(definition + " is running!");

            slackService.sendDirectMessage("test", String.format("Job %s is running.", definition));
            slackService.sendWarning("test", "Hosea");
            return false;
        }
        redisUtil.saveBooleanValue(definition, true, timeoutDuration, TimeUnit.MINUTES);
        return true;
    }

    private void unlockJob(String jobName) {
        try {
            redisUtil.deleteBooleanKey(jobName);
        } catch (Exception ex) {
            log.error(String.format("Unlock job %s exception, cause: %s", jobName, ex));

            slackService.sendDirectMessage("test", String.format("Unlock job %s exception, cause: %s", jobName, ex));
            slackService.sendWarning("test", "Hosea");
        }
    }
}