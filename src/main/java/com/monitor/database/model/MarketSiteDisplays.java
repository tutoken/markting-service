package com.monitor.database.model;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Map;


@Entity
@Table(name = "market_site_displays")
@Data
public class MarketSiteDisplays implements Serializable {

    @Id
    private int id;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean enable;

    public enum Type {
        announcement
    }

    public Map<String, String> getDisplays() {
        String content = this.content == null ? "" : this.content;
        String enable = Boolean.toString(this.enable);

        return Map.of("content", content, "enable", enable);
    }
}