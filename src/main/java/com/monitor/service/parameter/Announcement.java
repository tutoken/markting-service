package com.monitor.service.parameter;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Announcement implements Serializable {

    String content;
    boolean enable;

    public Announcement(String content, boolean enable) {
        this.content = content;
        this.enable = enable;
    }
}
