package com.monitor.schedule.definition;

import com.monitor.constants.Monitor;
import com.monitor.constants.Slack;
import com.monitor.constants.Token.TUSD;
import com.monitor.constants.Web3Provider;
import com.monitor.service.ServiceContext;
import com.monitor.service.interfaces.DailyReportService;
import com.monitor.service.interfaces.MarketSiteService;
import com.monitor.service.interfaces.SlackService;
import com.monitor.service.interfaces.Web3Service;
import com.monitor.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class ScheduleJobDefinition {

    public volatile boolean running;

    @Autowired
    protected MarketSiteService marketSiteService;

    @Autowired
    protected DailyReportService dailyReportService;

    @Autowired
    protected Web3Service web3Service;

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

    public void start() {
        if (this.running) {
            throw new RuntimeException("CurrentJosIsRunning!");
        }
        this.running = true;
    }

    public void end() {
        this.running = false;
    }
}
