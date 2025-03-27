package com.example.personal_finance_tracker.app.interfaces;

import com.example.personal_finance_tracker.app.models.FinanceEntry;

import java.util.List;

public interface FinanceEntryRepoInterface {
    void deleteById(Long id);
    FinanceEntry create(FinanceEntry entry);
    FinanceEntry update(Long id, FinanceEntry entry) throws Exception;
    List<FinanceEntry> findAll();
    List<FinanceEntry> findByType(String type);
    List<FinanceEntry> findByUserId(Long userId);
    List<FinanceEntry> findByTypeAndUserId(String type, Long userId);
    List<Object[]> findCategoryWiseSpendingForCurrentMonth(Long userId);
    List<Object[]> findCategoryWiseIncomeForCurrentMonth(Long userId);
    FinanceEntry save(FinanceEntry entry);
}
