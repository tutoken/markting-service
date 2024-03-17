package com.monitor.database.model;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Data
@Table(name = "schedule_job_detail")
//@TypeDef(name = "string-array", typeClass = StringArrayType.class)
public class SchedulerJobDetail implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "definition")
    private String definition;

    @Column(name = "description")
    private String description;

    @Column(name = "timeout_duration")
    private int timeoutDuration;

    /**
     * 0-enabled
     * 1-disabled
     */
    @Column(name = "status")
    private int status;

    /**
     * 0-alarm
     * 1-mute
     */
    @Column(name = "mute")
    private int mute;

    @Column(name = "channel")
    private String channel;

    //    @Type(type = "string-array")
    //    @Column(name = "notification_recipient", columnDefinition = "text[]")
    //    private List<String> recipients;

    @Column(name = "notification_recipient")
    private String recipients;
}