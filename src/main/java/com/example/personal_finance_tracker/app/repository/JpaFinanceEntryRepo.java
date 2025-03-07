package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.interfaces.FinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.JpaFinanceEntry;
import com.example.personal_finance_tracker.app.services.FinanceEntryMapper;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.example.personal_finance_tracker.app.interfaces.JpaFinanceEntryRepoInterface;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JpaFinanceEntryRepo implements FinanceEntryRepoInterface {
    private final JpaFinanceEntryRepoInterface jpaRepo;
    private final FinanceEntryMapper financeMapper;

    @Override
    public void deleteById (Long id) {
        jpaRepo.deleteById(id);
    }

    @Override
    public FinanceEntry create (FinanceEntry entry) {
        JpaFinanceEntry jpaEntry = financeMapper.toJpaFinanceEntry(entry);
        JpaFinanceEntry savedJpaEntry = jpaRepo.save(jpaEntry);
        return financeMapper.toFinanceEntry(savedJpaEntry);
    }

    @Override
    public FinanceEntry update (Long id, FinanceEntry financeEntry) throws Exception {
        JpaFinanceEntry jpaFinanceEntry = jpaRepo.findById(id)
                .orElseThrow(() -> new Exception("No entry found with the id" + id));
        jpaFinanceEntry.setLabel(financeEntry.getLabel());
        jpaFinanceEntry.setType(financeEntry.getType());
        jpaFinanceEntry.setAmount(financeEntry.getAmount());
        jpaFinanceEntry.setCategory(financeEntry.getCategory());
        jpaFinanceEntry.setDate(financeEntry.getDate());

        JpaFinanceEntry savedJpaEntry = jpaRepo.save(jpaFinanceEntry);
        return financeMapper.toFinanceEntry(savedJpaEntry);
    }

    @Override
    public List<FinanceEntry> findAll() {
        return jpaRepo.findAll().stream()
                .map(financeMapper::toFinanceEntry)
                .toList();
    }

    @Override
    public List<FinanceEntry> findByType (String type) {
        return jpaRepo.findByType(type).stream()
                .map(financeMapper::toFinanceEntry)
                .toList();
    }
}
