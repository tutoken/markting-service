package com.monitor.service.impl.common;

import com.monitor.constants.Monitor;
import com.monitor.constants.Slack;
import com.monitor.constants.Web3Provider;
import com.monitor.service.interfaces.SlackService;
import com.monitor.utils.CommonUtil;
import com.monitor.utils.HttpUtil;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.ButtonElement;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.slack.api.Slack.getInstance;
import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;


@Service("slackService")
public class SlackServiceImpl implements SlackService {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private static final ConcurrentHashMap<String, Map<String, String>> MESSAGES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Map<String, String>> DETAILS = new ConcurrentHashMap<>();
    private static final Set<String> WARNINGS = Collections.synchronizedSet(new HashSet<>());
    private static final ConcurrentHashMap<String, String> TOTAL_SUPPLIES = new ConcurrentHashMap<>();

    private static final List<String[]> TITLE = Arrays.asList(
//            new String[]{"Chain", "totalSupply", "Successful option", "Unsuccessful option"},
            new String[]{"Chain", "ratifiedMintPool", "multiSigMintPool", "instantMintPool"},
            new String[]{"Account", "Chain", "Balance"});

    @Autowired
    private Slack slack;

    @Autowired
    private Monitor monitor;

    @Autowired
    private Web3Provider web3Provider;

    @Override
    public void init() {
        MESSAGES.clear();
        WARNINGS.clear();
        DETAILS.clear();
        TOTAL_SUPPLIES.clear();
    }

    @Override
    public String sendSlackPost(String slackWebhook, String jsonStringBo) {
        return HttpUtil.post(slackWebhook, jsonStringBo);
    }

    @Override
    public String send(String webhookUrl, Payload payload) {
        WebhookResponse response = null;
        try {
            response = getInstance().send(webhookUrl, payload);
        } catch (IOException e) {
            logger.error(e.toString(), e);
        }
        if (response != null) {
            logger.info(response.getBody());
            return response.getBody();
        }
        return null;
    }

    @Override
    public void sendNotice(String channel, String message) {
        Payload.PayloadBuilder payloadBuilder = Payload.builder();
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(section(section -> section.text(markdownText(message))));
        try {
            String response = this.send(slack.getWebhook(channel), payloadBuilder.text("").blocks(blocks).build());
            logger.info(response);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    @Override
    public void putDivide(String channel) {
        Payload.PayloadBuilder payloadBuilder = Payload.builder();
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(divider());
        this.send(slack.getWebhook(channel), payloadBuilder.text("").blocks(blocks).build());
    }

    @Override
    public void addMessage(String chain, Map<String, String> message) {
        MESSAGES.merge(chain, message, (oldValue, newValue) -> {
            oldValue.putAll(newValue);
            return oldValue;
        });
    }

    @Override
    public void addDetail(String chain, Map<String, String> detail) {
        DETAILS.merge(chain, detail, (oldValue, newValue) -> {
            oldValue.putAll(newValue);
            return oldValue;
        });
    }

    @Override
    public void addTotalSupply(Map<String, String> message) {
        TOTAL_SUPPLIES.putAll(message);
    }

    @Override
    public void addWarning(String warning) {
        WARNINGS.add(warning);
    }

    @Override
    public void sendButton(String text, String url) {
        List<LayoutBlock> buttons = new ArrayList<>();
        Payload.PayloadBuilder payloadBuilder = Payload.builder();
        buttons.add(actions(actions -> actions.elements(Collections.singletonList(ButtonElement.builder()
                .text(PlainTextObject.builder().text(text).build()).url(url).build()))));
        this.send(slack.getWebhook("tusd"), payloadBuilder.text("").blocks(buttons).build());
    }

    @Override
    public void sendAsTable(String channel) {
        Payload.PayloadBuilder payloadBuilder = Payload.builder();
        List<LayoutBlock> totalSupply = new ArrayList<>();

        String natively = createTable(new String[]{"Chain", "totalSupply"}, monitor.getNativelyChains());
        if (natively != null) {
            List<String> listNativelyValues = TOTAL_SUPPLIES.entrySet().stream()
                    .filter(entry -> monitor.getNativelyChains().contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
            totalSupply.add(section(section -> section.text(markdownText("```" + "Natively Networks TotalSupply: " + CommonUtil.sum(listNativelyValues) + "\n" + natively + "```"))));
            this.send(slack.getWebhook(channel), payloadBuilder.text("").blocks(totalSupply).build());
        }

        String bridged = createTable(new String[]{"Chain", "totalSupply"}, monitor.getBridgedChains());
        if (bridged != null) {
            totalSupply = new ArrayList<>();
            List<String> listNativelyValues = TOTAL_SUPPLIES.entrySet().stream()
                    .filter(entry -> monitor.getBridgedChains().contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
            totalSupply.add(section(section -> section.text(markdownText("```" + "Bridged Networks TotalSupply: " + CommonUtil.sum(listNativelyValues) + "\n" + bridged + "```"))));
            this.send(slack.getWebhook(channel), payloadBuilder.text("").blocks(totalSupply).build());
        }

        String transactions = createTable(new String[]{"Chain", "Successful option", "Unsuccessful option"}, monitor.getNativelyChains());
        if (transactions != null) {
            List<LayoutBlock> trx = new ArrayList<>();
            trx.add(section(section -> section.text(markdownText("```" + transactions + "```"))));
            this.send(slack.getWebhook(channel), payloadBuilder.text("").blocks(trx).build());
        }

        TITLE.forEach(title -> {
            List<LayoutBlock> blocks = new ArrayList<>();
            String pools = createTable(title, null);

            if (pools != null) {
                blocks.add(section(section -> section.text(markdownText("```" + pools + "```"))));
            }
            if (!CollectionUtils.isEmpty(blocks)) {
                this.send(slack.getWebhook(channel), payloadBuilder.text("").blocks(blocks).build());
            }
        });

        DETAILS.forEach((key, detail) -> {
            if (detail != null) {
                this.send(slack.getWebhook(channel), payloadBuilder.text("").blocks(List.of((section(section -> section.text(markdownText(key + "\n")))))).build());
                detail.forEach((hash, operation) -> {
                    List<LayoutBlock> blocks = new ArrayList<>();
                    blocks.add(section(section -> section.text(markdownText("```Hash: " + hash + "\nOperation: " + operation + "```"))));
                    this.send(slack.getWebhook(channel), payloadBuilder.text("").blocks(blocks).build());
                });
            }
        });

        if (!CollectionUtils.isEmpty(WARNINGS)) {
            List<LayoutBlock> blocks = new ArrayList<>();
            blocks.add(section(section -> section.text(markdownText(WARNINGS.stream().map(s -> "\n" + s).collect(Collectors.joining())))));
            this.send(slack.getWebhook(channel), payloadBuilder.text("").blocks(blocks).build());
        }
    }

    private List<Map<String, String>> createMessage(String[] title, Set<String> filter) {
        List<Map<String, String>> table = new LinkedList<>();
        Map<String, String> header = new LinkedHashMap<>();
        for (String t : title) {
            header.put(t, t);
        }
        table.add(header);

        for (Map.Entry<String, Map<String, String>> entry : MESSAGES.entrySet()) {
            Map<String, String> row = new LinkedHashMap<>();
            String key = entry.getKey();
            if (!CollectionUtils.isEmpty(filter) && !filter.contains(key)) {
                continue;
            }
            row.put(title[0], web3Provider.chainName(key));
            Map<String, String> values = entry.getValue();
            if (values == null) {
                continue;
            }
            boolean empty = true;
            for (int i = 1; i < title.length; i++) {
                String value = values.getOrDefault(title[i], "N/A");
                if (!"N/A".equals(value)) {
                    empty = false;
                }
                row.put(title[i], value);
            }
            if (!empty) {
                table.add(row);
            }
        }

        return table.size() == 1 ? null : table;
    }

    private String createTable(String[] title, Set<String> filter) {
        List<Map<String, String>> table = this.createMessage(title, filter);
        if (table == null) {
            return null;
        }
        int[] maxWidth = new int[title.length];
        for (int i = 0; i < title.length; i++) {
            for (Map<String, String> row : table) {
                maxWidth[i] = Math.max(maxWidth[i], row.get(title[i]).length());
            }
        }

        StringBuilder separator = new StringBuilder("+");
        for (int i = 0; i < title.length; i++) {
            separator.append("-".repeat(maxWidth[i] + 2)).append("+");
        }
        StringBuilder result = new StringBuilder(separator).append("\n");

        for (Map<String, String> row : table) {
            result.append("| ");
            for (int j = 0; j < title.length; j++) {
                result.append(String.format("%-" + maxWidth[j] + "s | ", row.get(title[j])));
            }
            result.append("\n").append(separator).append("\n");
        }

        return result.toString();
    }

}
