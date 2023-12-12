package com.monitor.database.model;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Data
@Table(name = "daily_report")
public class DailyReport implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_token")
    private BigDecimal totalToken;

    @Column(name = "total_trust")
    private BigDecimal totalTrust;

    @Column(name = "ripcord")
    private String ripcord;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "report_file")
    private String report_file;

    @Column(name = "tokens")
//    @Convert(converter = JsonbConverter.class)
    private String tokens;

//    @Converter(autoApply = true)
//    public static class JsonbConverter implements AttributeConverter<String, JSONObject> {
//
//        @Override
//        public JSONObject convertToDatabaseColumn(String attribute) {
//            return JSONObject.parseObject(attribute);
//        }
//
//        @Override
//        public String convertToEntityAttribute(JSONObject dbData) {
//            return dbData.toJSONString();
//        }
//    }
}