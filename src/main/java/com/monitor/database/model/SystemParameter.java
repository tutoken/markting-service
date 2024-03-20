package com.monitor.database.model;

import lombok.Data;

import javax.annotation.PostConstruct;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Data
@Table(name = "system_parameters")
public class SystemParameter implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "value")
    private String value;

    @Column(name = "description")
    private String description;
}