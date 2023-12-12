package com.monitor.service.interfaces;

import com.slack.api.webhook.Payload;

import java.util.Map;

public interface SlackService {

    /**
     * Initialize, clear all unsent messages
     */
    void init();

    String sendSlackPost(String slackWebhook, String jsonStringBo) throws Exception;

    String send(String slackWebhook, Payload payload);

    void sendNotice(String channel, String message);

    void putDivide(String channel);

    void sendAsTable(String channel);

    void addMessage(String chain, Map<String, String> message);

    void addDetail(String hash, Map<String, String> message);

    void addTotalSupply(Map<String, String> message);

    void addWarning(String warning);

    void sendButton(String text, String url);
}
