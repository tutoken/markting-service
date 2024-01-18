package com.monitor.service.interfaces;

import com.monitor.service.parameter.Message;

public interface SlackService {

    String createScheduleMessage(String channel, String message, Integer postAt);

    String cancelScheduleMessage(String messageId);

    void sendCodeBlockMessage(String channel, String message);

    void sendDirectMessage(String channel, String message);

    void sendMessage(String channel, Message message);

    void flush();
}
