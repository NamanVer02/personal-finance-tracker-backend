package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.interfaces.FinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.interfaces.JpaFinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.JpaFinanceEntry;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.services.FinanceEntryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaFinanceEntryRepo implements FinanceEntryRepoInterface {
    private final JpaFinanceEntryRepoInterface jpaRepo;
    private final FinanceEntryMapper financeMapper;
    private final UserRepo userRepo;

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
        
        // Preserve the user association - don't allow changing the user
        // If userId is provided in the update, verify it matches the existing user
        if (financeEntry.getUserId() != null && jpaFinanceEntry.getUser() != null && 
            !financeEntry.getUserId().equals(jpaFinanceEntry.getUser().getId())) {
            throw new Exception("Cannot change the user associated with a finance entry");
        }

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
    
    @Override
    public List<FinanceEntry> findByUserId(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        // Return empty list if user not found
        return userOpt.map(user -> jpaRepo.findByUser(user).stream()
                .map(financeMapper::toFinanceEntry)
                .toList()).orElseGet(List::of);
    }
    
    @Override
    public List<FinanceEntry> findByTypeAndUserId(String type, Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        // Return empty list if user not found
        return userOpt.map(user -> jpaRepo.findByTypeAndUser(type, user).stream()
                .map(financeMapper::toFinanceEntry)
                .toList()).orElseGet(List::of);
    }

    @Override
    public List<Object[]> findCategoryWiseSpendingForCurrentMonth(Long userId) {
        return jpaRepo.findCategoryWiseSpendingForCurrentMonth(userId);
    }

    @Override
    public List<Object[]> findCategoryWiseIncomeForCurrentMonth(Long userId) {
        return jpaRepo.findCategoryWiseIncomeForCurrentMonth(userId);
    }


}
