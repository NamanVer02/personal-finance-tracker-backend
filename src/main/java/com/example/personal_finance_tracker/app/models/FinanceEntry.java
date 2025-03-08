package com.example.personal_finance_tracker.app.models;

import jakarta.persistence.Id;
import java.util.Date;
import lombok.Data;

@Data
public class FinanceEntry {
    @Id
    private Long id;

    private String label;
    private String type;
    private Double amount;
    private String category;
    private Date date;
    
    private Long userId;
}
