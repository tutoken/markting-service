package com.monitor.service.parameter;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommonResponse implements Serializable {
    private boolean success;
    private String message;

    public CommonResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
