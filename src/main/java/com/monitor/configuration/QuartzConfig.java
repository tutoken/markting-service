package com.monitor.configuration;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.AdaptableJobFactory;

@Configuration
public class QuartzConfig {
    @Autowired
    private CustomJobFactory customJobFactory;

    @SneakyThrows
    @Bean
    public Scheduler scheduler() {
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        Scheduler scheduler = schedulerFactory.getScheduler();
        scheduler.setJobFactory(customJobFactory);
        scheduler.start();
        return scheduler;
    }

    @Configuration
    static class CustomJobFactory extends AdaptableJobFactory {

        @Autowired
        private AutowireCapableBeanFactory autowireCapableBeanFactory;

        @NotNull
        @Override
        protected Object createJobInstance(@NotNull TriggerFiredBundle bundle) throws Exception {
            Object jobInstance = super.createJobInstance(bundle);
            autowireCapableBeanFactory.autowireBean(jobInstance);
            return jobInstance;
        }
    }
}
