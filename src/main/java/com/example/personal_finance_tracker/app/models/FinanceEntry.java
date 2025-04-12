package com.example.personal_finance_tracker.app.models;

import com.example.personal_finance_tracker.app.annotations.Loggable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Setter;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "finance_entries")
@Loggable
public class FinanceEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Label is required")
    @Size(min = 3, max = 255, message = "Label must be between 3 and 255 characters")
    @Column(name = "label", columnDefinition = "VARCHAR(255)")
    private String label;

    @NotNull(message = "Type is required")
    @Pattern(regexp = "^(Income|Expense)$", message = "Type must be either 'Income' or 'Expense'")
    @Column(name = "type")
    private String type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(name = "amount")
    private Double amount;

    @NotBlank(message = "Category is required")
    @Column(name = "category")
    private String category;

    @NotNull(message = "Date is required")
    @PastOrPresent(message = "Date cannot be in the future")
    @Column(name = "entry_date")
    @Temporal(TemporalType.DATE)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "financeEntries"})
    private User user;

    // Setter for userId to maintain backward compatibility
    // Transient field to maintain compatibility with existing code
    @Setter
    @Transient
    private Long userId;
    
    // Getter for userId that uses the user object if available
    public Long getUserId() {
        if (userId != null) {
            return userId;
        }
        return user != null ? user.getId() : null;
    }

    @Version
    private Long version;
}
