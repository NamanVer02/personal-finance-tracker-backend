package com.example.personal_finance_tracker.app.models;

import com.example.personal_finance_tracker.app.annotations.Encode;
import com.example.personal_finance_tracker.app.annotations.Loggable;
import com.example.personal_finance_tracker.app.config.StringEncodeConverter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "finance_entries")
@Loggable
public class FinanceEntry {
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
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "financeEntries"})
    private User user;
    
    // Transient field to maintain compatibility with existing code
    @Transient
    private Long userId;
    
    // Getter for userId that uses the user object if available
    public Long getUserId() {
        if (userId != null) {
            return userId;
        }
        return user != null ? user.getId() : null;
    }
    
    // Setter for userId to maintain backward compatibility
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
