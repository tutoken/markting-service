package com.monitor.database.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "ecosystem_item")
@Data
public class EcosystemItem implements Serializable {

    @Id
    //    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ecosystem_item_id_seq")
//    @SequenceGenerator(name = "ecosystem_item_id_seq", sequenceName = "ecosystem_item_id_seq")
    private int id;

    @Column
    private int order;

    @Column(nullable = false)
    private int status;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String name;

//    @OneToMany
//    private List<EcosystemDetail> ecosystemDetails;
}