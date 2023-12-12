package com.monitor.constants;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;
import java.util.Map;

public class Token {

    @Data
    @Configuration
    @PropertySource("classpath:token.properties")
    @ConfigurationProperties(prefix = "web3.token.tusd")
    public static class TUSD {
        private final Map<String, String> contract = new HashMap<>();
        private final Map<String, String> controller = new HashMap<>();
        private final Map<String, String> owner = new HashMap<>();
        private final Map<String, String> lock = new HashMap<>();

        public String getContract(String name) {
            return this.contract.getOrDefault(name, null);
        }

        public String getController(String name) {
            return this.controller.getOrDefault(name, null);
        }

        public String getOwner(String name) {
            return this.owner.getOrDefault(name, null);
        }

        public String getLock(String name) {
            return this.lock.getOrDefault(name, null);
        }
    }
}
