package com.monitor.schedule.base;

import com.monitor.constants.Monitor;
import com.monitor.constants.Slack;
import com.monitor.constants.Token.TUSD;
import com.monitor.constants.Web3Provider;
import com.monitor.database.model.SchedulerJobDetail;
import com.monitor.service.ServiceContext;
import com.monitor.service.interfaces.SlackService;
import com.monitor.service.interfaces.TokenService;
import com.monitor.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class ScheduleJobDefinition {

    @Autowired
    protected TokenService tokenService;

    @Autowired
    protected ServiceContext serviceContext;

    @Autowired
    protected Web3Provider web3Provider;

    @Autowired
    protected SlackService slackService;

    @Autowired
    protected Monitor monitor;

    @Autowired
    protected Slack slack;

    @Autowired
    protected TUSD token;

    @Autowired
    protected RedisUtil redisUtil;

    protected long currentTime;

    protected SchedulerJobDetail schedulerJobDetail;

    abstract protected void run();

    public void execute(long currentTime, SchedulerJobDetail schedulerJobDetail) {
        this.currentTime = currentTime;
        this.schedulerJobDetail = schedulerJobDetail;
        this.run();
    }
}
