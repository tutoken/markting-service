package com.monitor.schedule;

import com.monitor.constants.Slack;
import com.monitor.database.model.SchedulerJob;
import com.monitor.database.model.SchedulerJobDetail;
import com.monitor.database.repository.SchedulerJobDetailRepository;
import com.monitor.database.repository.SchedulerJobRepository;
import com.monitor.database.repository.SystemParametersRepository;
import com.monitor.schedule.base.ScheduleTaskExecutor;
import com.monitor.service.parameter.SchedulerResponse;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class ScheduleTaskController {

    @Autowired
    private SchedulerJobRepository schedulerJobRepository;

    @Autowired
    private SchedulerJobDetailRepository schedulerJobDetailRepository;

    @Autowired
    private ScheduleTaskExecutor scheduleTaskExecutor;

    @Autowired
    private Slack slack;

    @Autowired
    private SystemParametersRepository systemParametersRepository;

    private Scheduler scheduler;

    public void execute(List<String> definitions) {
        List<SchedulerJobDetail> schedulerJobDetails = schedulerJobDetailRepository.findByDefinitionIn(definitions);
        if (CollectionUtils.isEmpty(schedulerJobDetails)) {
            return;
        }
        scheduleTaskExecutor.execute(schedulerJobDetails);
    }

    private static final Map<String, Class<? extends ScheduleTaskExecutor>> map = new ConcurrentReferenceHashMap<>();

    @PostConstruct
    public void init() {
        initSchedulerJobs();
    }


    public void initSchedulerJobs() {
        this.getScheduler();

        List<SchedulerJob> schedulerJobs = schedulerJobRepository.findAll();

        if (CollectionUtils.isEmpty(schedulerJobs)) {
            return;
        }

        try {
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyGroup());
            for (JobKey jobKey : jobKeys) {
                scheduler.deleteJob(jobKey);
            }
        } catch (SchedulerException e) {
            log.error("Can not delete scheduler", e);
            return;
        }

        schedulerJobs.forEach(schedulerJob -> {
            if (schedulerJob == null) {
                return;
            }
            JobDataMap map = new JobDataMap();
            String description = schedulerJob.getDescription();
            long groupId = schedulerJob.getId();
            String executor = schedulerJob.getExecutor();

            try {
                JobDetail jobDetail = JobBuilder.newJob(getClass(executor)).withIdentity(String.valueOf(groupId), description).setJobData(map).build();
                CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(schedulerJob.getCronExpression());
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(String.valueOf(groupId), description).withSchedule(cronScheduleBuilder).build();

                scheduler.scheduleJob(jobDetail, trigger);

            } catch (SchedulerException | ClassNotFoundException e) {
                log.error(String.format("Create scheduler job %s failed.", schedulerJob.getDescription()), e);
            }
        });
        try {
            scheduler.start();
        } catch (SchedulerException e) {
            log.error("Failed to start scheduler.", e);
        }
    }

    private Class<? extends ScheduleTaskExecutor> getClass(String jobName) throws ClassNotFoundException {
        if (map.containsKey(jobName)) {
            return map.get(jobName);
        }
        Class<? extends ScheduleTaskExecutor> clazz = Class.forName(String.format("com.monitor.schedule.base.%s", jobName)).asSubclass(ScheduleTaskExecutor.class);
        map.put(jobName, clazz);

        return clazz;
    }

    public SchedulerResponse getSchedulerResult() {
        this.getScheduler();
        SchedulerResponse response = new SchedulerResponse();
        try {
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
            for (JobKey jobKey : jobKeys) {
                Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
                Date nextFireTime = trigger.getNextFireTime();

                long groupId = Long.parseLong(jobKey.getName());
                List<SchedulerJobDetail> schedulerJobDetails = schedulerJobDetailRepository.findByGroupIdOrderByPriority(groupId);
                response.add(jobKey.getGroup(), schedulerJobDetails, nextFireTime.toString());
                response.setStarted(scheduler.isStarted());
            }
        } catch (SchedulerException e) {
            log.error("Get scheduler job details error.");
            throw new RuntimeException(e);
        }

        return response;
    }

    private void getScheduler() {
        if (scheduler == null) {
            try {
                scheduler = StdSchedulerFactory.getDefaultScheduler();
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
