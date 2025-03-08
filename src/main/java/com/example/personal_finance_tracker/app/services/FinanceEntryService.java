package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.interfaces.FinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.models.FinanceEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceEntryService {
    private final FinanceEntryRepoInterface financeEntryRepo;

    public void deleteById(Long id) {
        financeEntryRepo.deleteById(id);
    }

    public FinanceEntry create (FinanceEntry entry){
        return financeEntryRepo.create(entry);
    }

    public FinanceEntry update (Long id, FinanceEntry entry) throws Exception {
        return financeEntryRepo.update(id, entry);
    }

    public List<FinanceEntry> findAll() {
        return financeEntryRepo.findAll();
    }

    public List<FinanceEntry> findByType(String type) {
        return financeEntryRepo.findByType(type);
    }

    public List<FinanceEntry> findByUserId(Long userId) {
        return financeEntryRepo.findByUserId(userId);
    }

    public List<FinanceEntry> findByTypeAndUserId(String type, Long userId) {
        return financeEntryRepo.findByTypeAndUserId(type, userId);
    }
}
