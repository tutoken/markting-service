package com.monitor.service.impl.common;

import com.monitor.constants.Slack;
import com.monitor.database.model.SystemParameter;
import com.monitor.database.repository.SystemParametersRepository;
import com.monitor.service.interfaces.SlackService;
import com.monitor.service.parameter.Message;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.slack.api.Slack.getInstance;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;

@Slf4j
@Service("slackService")
public class SlackServiceImpl implements SlackService {

    @Autowired
    private SystemParametersRepository systemParametersRepository;
    private final ConcurrentHashMap<String, Deque<String>> queue = new ConcurrentHashMap<>();


    @Autowired
    private Slack slack;

    @Override
    public String createScheduleMessage(String channel, String message, Integer postAt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String cancelScheduleMessage(String messageId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void sendCodeBlockMessage(String channel, String message) {
        try {
            this.send(channel, String.format("```%s```", message));
        } catch (Exception ex) {
            log.error(String.format("Send message to  %s failed, message is %s", channel, message), ex);
        }
    }

    @Override
    public void sendDirectMessage(String channel, String message) {
        try {
            this.send(channel, message);
        } catch (Exception ex) {
            log.error(String.format("Send message to  %s failed, message is %s", channel, message), ex);
        }
    }

    @Override
    public void sendWarning(String channel, String... memberIds) {
        if (memberIds.length < 1) {
            return;
        }
        String warningMembers = Arrays.stream(memberIds).map(memberId -> slack.getID(memberId)).collect(Collectors.joining());
        String message = String.format("%s%s", Slack.WARNING, warningMembers);
        try {
            this.send(channel, String.format("%s%s", Slack.WARNING, warningMembers));
        } catch (Exception ex) {
            log.error(String.format("Send warning to  %s failed, detail is %s", channel, message), ex);
        }
    }

    @Override
    public void sendMessage(String channel, Message message) {
        Assert.isTrue(!CollectionUtils.isEmpty(message), "Message should not be empty");

        message.forEach(value -> {
            try {
                this.send(channel, value);
            } catch (Exception ex) {
                log.error(String.format("Send message to  %s failed, message is %s", channel, value), ex);
            }
        });
    }

    private void addMessage(String channel, String message) {
        this.queue.computeIfAbsent(channel, k -> new ArrayDeque<>()).add(message);
    }

    private void send(String channel, String message) {
        Assert.notNull(channel, "Channel must not be null");
        Assert.notNull(message, "Message must not be null");

        try {
            WebhookResponse response = getInstance().send(slack.getWebhook(channel), this.createPayload(message));
            if (response != null) {
                log.info(String.format("%s %d", response.getBody(), response.getCode()));
            }
        } catch (IOException ex) {
            this.addMessage(channel, message);
            log.error(String.format("Send message to  %s failed", channel), ex);
        }
    }

    @Override
    public void flush() {
        queue.forEach((channel, deque) -> {
            while (!deque.isEmpty()) {
                try {
                    getInstance().send(slack.getWebhook(channel), createPayload(deque.removeLast()));
                } catch (IOException e) {
                    log.error(String.format("Retry send message to  %s failed", channel), e);
                }
            }
        });
    }

    @Override
    @PostConstruct
    public void init() {
        List<SystemParameter> systemParameters = systemParametersRepository.findAll();

        Map<String, Map<String, String>> groupedByNameValue = systemParameters.stream()
                .collect(Collectors.groupingBy(SystemParameter::getType,
                        Collectors.toMap(SystemParameter::getName, SystemParameter::getValue)));

        slack.setWebhook(groupedByNameValue.get("slack_webhook"));
        slack.setNotice(groupedByNameValue.get("slack_member"));
    }

    @Override
    public Map<String, String> list() {
        Map<String, String> slackMap = new HashMap<>();

        slackMap.putAll(slack.getWebhook());
        slackMap.putAll(slack.getNotice());

        return slackMap;
    }

    private Payload createPayload(String message) {
        Payload.PayloadBuilder payloadBuilder = Payload.builder();
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(section(section -> section.text(markdownText(message))));

        return payloadBuilder.text("").blocks(blocks).build();
    }
}
