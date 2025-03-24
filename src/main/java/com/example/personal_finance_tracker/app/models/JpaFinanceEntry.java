package com.example.personal_finance_tracker.app.models;

import com.example.personal_finance_tracker.app.annotations.Loggable;
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

    private String label;
    private String type;
    private Double amount;
    private String category;
    private Date date;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
