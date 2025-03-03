package com.example.personal_finance_tracker.app.services;

import org.springframework.stereotype.Component;
import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.JpaFinanceEntry;

@Component
public class FinanceEntryMapper {
    public JpaFinanceEntry toJpaFinanceEntry (FinanceEntry financeEntry) {
        JpaFinanceEntry JpaEntry = new JpaFinanceEntry();
        JpaEntry.setId(financeEntry.getId());
        JpaEntry.setLabel(financeEntry.getLabel());
        JpaEntry.setType(financeEntry.getType());
        JpaEntry.setAmount(financeEntry.getAmount());
        JpaEntry.setCategory(financeEntry.getCategory());
        JpaEntry.setDate(financeEntry.getDate());
        return JpaEntry;
    }

    public FinanceEntry toFinanceEntry (JpaFinanceEntry JpaEntry) {
        FinanceEntry financeEntry = new FinanceEntry();
        financeEntry.setId(JpaEntry.getId());
        financeEntry.setLabel(JpaEntry.getLabel());
        financeEntry.setType(JpaEntry.getType());
        financeEntry.setAmount(JpaEntry.getAmount());
        financeEntry.setCategory(JpaEntry.getCategory());
        financeEntry.setDate(JpaEntry.getDate());
        return financeEntry;
    }
}
