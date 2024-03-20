package com.monitor.service.interfaces;

import com.monitor.service.parameter.Message;

import java.util.Map;

public interface SlackService {

    String createScheduleMessage(String channel, String message, Integer postAt);

    String cancelScheduleMessage(String messageId);

    void sendCodeBlockMessage(String channel, String message);

    void sendDirectMessage(String channel, String message);

    void sendWarning(String channel, String... memberIds);

    void sendMessage(String channel, Message message);

    void flush();

    void init();

    Map<String, String> list();
}
