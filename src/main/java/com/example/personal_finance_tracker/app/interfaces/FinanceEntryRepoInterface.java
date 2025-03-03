package com.example.personal_finance_tracker.app.interfaces;

import java.util.List;
import com.example.personal_finance_tracker.app.models.FinanceEntry;

public interface FinanceEntryRepoInterface {
    void deleteById(Long id);
    FinanceEntry create (FinanceEntry entry);
    FinanceEntry update (Long id, FinanceEntry entry) throws Exception;
    List<FinanceEntry> findAll ();
    List<FinanceEntry> findByType (String type);
}
