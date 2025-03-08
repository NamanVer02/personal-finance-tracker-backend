package com.example.personal_finance_tracker.app.services;

import org.springframework.stereotype.Component;
import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.JpaFinanceEntry;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class FinanceEntryMapper {
    @Autowired
    private UserRepo userRepo;
    
    public JpaFinanceEntry toJpaFinanceEntry(FinanceEntry financeEntry) {
        JpaFinanceEntry jpaEntry = new JpaFinanceEntry();
        jpaEntry.setId(financeEntry.getId());
        jpaEntry.setLabel(financeEntry.getLabel());
        jpaEntry.setType(financeEntry.getType());
        jpaEntry.setAmount(financeEntry.getAmount());
        jpaEntry.setCategory(financeEntry.getCategory());
        jpaEntry.setDate(financeEntry.getDate());
        
        // Set user if userId is provided
        if (financeEntry.getUserId() != null) {
            userRepo.findById(financeEntry.getUserId()).ifPresent(jpaEntry::setUser);
        }
        
        return jpaEntry;
    }

    public FinanceEntry toFinanceEntry(JpaFinanceEntry jpaEntry) {
        FinanceEntry financeEntry = new FinanceEntry();
        financeEntry.setId(jpaEntry.getId());
        financeEntry.setLabel(jpaEntry.getLabel());
        financeEntry.setType(jpaEntry.getType());
        financeEntry.setAmount(jpaEntry.getAmount());
        financeEntry.setCategory(jpaEntry.getCategory());
        financeEntry.setDate(jpaEntry.getDate());
        
        // Set userId if user is present
        if (jpaEntry.getUser() != null) {
            financeEntry.setUserId(jpaEntry.getUser().getId());
        }
        
        return financeEntry;
    }
}
