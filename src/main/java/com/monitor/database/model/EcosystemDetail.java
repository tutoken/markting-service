package com.monitor.database.model;

import lombok.Data;
import lombok.Getter;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "ecosystem_detail")
@Getter
public class EcosystemDetail implements Serializable {

    @EmbeddedId
    private EcosystemDetailId id;

    @Column(nullable = false)
    private int status;

    @Column(nullable = false)
    private String value;

    public String getField() {
        return this.id.field;
    }

    public int getItemId() {
        return this.id.itemId;
    }

    @Embeddable
    @Data
    public static class EcosystemDetailId implements Serializable {

        @Column(nullable = false)
        private int itemId;

        @Column(nullable = false)
        private String field;
    }
}
