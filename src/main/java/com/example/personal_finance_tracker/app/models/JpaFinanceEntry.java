package com.example.personal_finance_tracker.app.models;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Data;


@Data
@Entity
@Table(name = "finance_entries")
public class JpaFinanceEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String label;
    private String type;
    private Double amount;
    private String category;
    private Date date;
}
