package com.monitor.service.parameter;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class TotalSupply implements Serializable {

    String label;
    BigDecimal value;

    public TotalSupply(String label, BigDecimal value) {
        this.label = label;
        this.value = value;
    }
}
