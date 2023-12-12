package com.monitor.constants;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@PropertySource("classpath:service.properties")
@ConfigurationProperties(prefix = "web3.provider")
@Slf4j
public class Web3Provider {

    private Map<String, Map<String, String>> url = new HashMap<>();

    private Map<String, Map<String, String>> key = new HashMap<>();

    public String getRpcUrl(String chain) {
        return this.url.get("rpc").get(chain);
    }

    public String getScanUrl(String chain) {
        return this.url.get("scan").get(chain);
    }

    public String getRpcKey(String chain) {
        return this.key.get("rpc").get(chain);
    }

    public String getScanKey(String chain) {
        return this.key.get("scan").get(chain);
    }

    public String chainName(String tag) {
        try {
            return BLOCK_CHAIN.valueOf(tag).getName();
        } catch (Exception exception) {
            log.warn(exception.getMessage());
        }
        return tag;
    }

    enum BLOCK_CHAIN {
        eth("Eth"), avax("Avax"), tron("Tron"),
        bnb("Bnb"), bsc("BSC"), bsc_old("BSC(Old)"),
        polygon("Polygon"), ftm("Fantom"), arbitrum("Arbitrum"),
        optimism("Optimism"), cronos("Cronos"), aurora("Aurora"),
        heco("HECO");

        private final String name;

        BLOCK_CHAIN(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}
