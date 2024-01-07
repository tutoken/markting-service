package com.monitor.schedule.definition;

import com.monitor.constants.Monitor;
import com.monitor.constants.Slack;
import com.monitor.constants.Token.TUSD;
import com.monitor.constants.Web3Provider;
import com.monitor.service.ServiceContext;
import com.monitor.service.interfaces.SlackService;
import com.monitor.service.interfaces.TokenService;
import com.monitor.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

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

    protected String lastTimeStamp;

    protected String currentTimeStamp;

    abstract protected void run();

    public void launch(String lastTimeStamp, String currentTimeStamp) {
        this.lastTimeStamp = lastTimeStamp;
        this.currentTimeStamp = currentTimeStamp;
        this.run();
    }
}
