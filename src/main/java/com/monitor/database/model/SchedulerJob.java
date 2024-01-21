package com.monitor.database.model;

import lombok.Builder;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Data
@Table(name = "schedule_job")
public class SchedulerJob implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "executor")
    private String executor;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "description")
    private String description;
}