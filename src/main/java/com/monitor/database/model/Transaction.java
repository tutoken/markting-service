package com.monitor.database.model;

import lombok.Builder;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Data
@Table(name = "transaction")
@Builder
public class Transaction implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chain")
    private String chain;

    @Column(name = "transaction_hash")
    private String transactionHash;

    @Column(name = "block_number")
    private String blockNumber;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "contract_address")
    private String contractAddress;

    @Column(name = "from_address")
    private String fromAddress;

    @Column(name = "to_address")
    private String toAddress;

    @Column(name = "type")
    private String type;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "gas")
    private String gas;

    @Column(name = "createdAt")
    private Timestamp createdAt;

    @Column(name = "nonce")
    private Timestamp nonce;

    @Column(name = "status")
    private String status;
}