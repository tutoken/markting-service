package com.monitor.constants;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@PropertySource("classpath:slack.properties")
@ConfigurationProperties(prefix = "slack")
public class Slack {

    private Map<String, String> webhook = new HashMap<>();

    private Map<String, String> notice = new HashMap<>();

    private Map<String, String> prefix = new HashMap<>();

    /**
     * generate an @ tag
     *
     * @param name
     * @return
     */
    public String getID(String name) {
        return this.notice.containsKey(name) ? " <@" + this.notice.get(name) + "> " : " <@" + this.notice.get("Hosea") + "> ";
    }

    /**
     * Used to generate hyperlinks for a specified address
     *
     * @param chain
     * @param hash
     * @param text
     * @return
     */
    public String getLink(String chain, String hash, String text) {
        return "<" + prefix.get(chain) + hash + "|" + text + ">";
    }

    /**
     * Used to generate hyperlinks
     *
     * @param link
     * @param tag
     * @return
     */
    public String getLink(String link, String tag) {
        return "<" + link + "|" + tag + ">";
    }

    /**
     * Get slack channel
     *
     * @param channel
     * @return
     */
    public String getWebhook(String channel) {
        return webhook.getOrDefault(channel, webhook.get("test"));
    }

    public static final String WARNING = ":warning:";

    public static final String COMPLETED = ":white_check_mark:";

    public static final Map<String, String> ICO = new HashMap<>();

    static {
        ICO.put("eth", ":ethereum:");
        ICO.put("tron", ":tronico:");
        ICO.put("bsc", ":binance:");
        ICO.put("bsc_old", ":binance:");
        ICO.put("avax", ":ava:");
        ICO.put("ftm", ":fantom:");
        ICO.put("polygon", ":polygon:");
        ICO.put("arbitrum", ":arbitrum:");
        ICO.put("optimism", ":optimism:");
        ICO.put("cronos", ":cronos:");
        ICO.put("aurora", ":aurora:");
    }
}
