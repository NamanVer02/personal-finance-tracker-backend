package com.example.personal_finance_tracker.app.models;

import com.example.personal_finance_tracker.app.annotations.Encode;
import com.example.personal_finance_tracker.app.annotations.Loggable;
import com.example.personal_finance_tracker.app.config.StringEncodeConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "finance_entries")
@Loggable
public class JpaFinanceEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Encode
    @Convert(converter = StringEncodeConverter.class)
    @Column(name = "label", columnDefinition = "VARCHAR(255)")
    private String label;

    @Encode
    @Convert(converter = StringEncodeConverter.class)
    @Column(name = "type")
    private String type;

//    @Encode
//    @Convert(converter = DoubleEncodeConverter.class)
    @Column(name = "amount")
    private Double amount;

    @Encode
    @Convert(converter = StringEncodeConverter.class)
    @Column(name = "category")
    private String category;

    @Encode
    @Convert(converter = StringEncodeConverter.class)
    @Column(name = "entry_date")
    @Temporal(TemporalType.DATE)
    private Date date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}