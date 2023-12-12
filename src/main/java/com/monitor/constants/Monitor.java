package com.monitor.constants;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Data
@Configuration
@PropertySource("classpath:monitor.properties")
@ConfigurationProperties(prefix = "web3.monitor")
public class Monitor {

    private Map<String, List<String>> address = new HashMap<>();

    private Map<String, String> balance = new HashMap<>();

    private Set<String> bridgedChains;

    private Set<String> nativelyChains;

    private Set<String> chains;

    public static final BigDecimal DECIMAL18 = new BigDecimal("1000000000000000000");

    public static final BigInteger INTEGER18 = new BigInteger("1000000000000000000");

    public static final BigDecimal TUSD_PRICE = new BigDecimal("0.999");

    /**
     * Addresses that need to be monitored
     *
     * @param chain
     * @return
     */
    public List<String> getAddress(String chain) {
        return this.address.get(chain);
    }

    /**
     * Lower limit of balance alarm
     *
     * @param chain
     * @return
     */
    public String getBalanceThreshold(String chain) {
        return this.balance.get(chain);
    }
}
