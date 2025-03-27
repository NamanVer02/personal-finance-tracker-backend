package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.interfaces.FinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.interfaces.JpaFinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaFinanceEntryRepo implements FinanceEntryRepoInterface {
    private final JpaFinanceEntryRepoInterface jpaRepo;
    private final UserRepo userRepo;

    @Override
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }

    @Override
    public FinanceEntry create(FinanceEntry entry) {
        if (entry.getUserId() != null) {
            userRepo.findById(entry.getUserId()).ifPresent(entry::setUser);
        }
        return jpaRepo.save(entry);
    }

    @Override
    public FinanceEntry update(Long id, FinanceEntry financeEntry) throws Exception {
        FinanceEntry existingEntry = jpaRepo.findById(id)
                .orElseThrow(() -> new Exception("No entry found with the id" + id));
        
        existingEntry.setLabel(financeEntry.getLabel());
        existingEntry.setType(financeEntry.getType());
        existingEntry.setAmount(financeEntry.getAmount());
        existingEntry.setCategory(financeEntry.getCategory());
        existingEntry.setDate(financeEntry.getDate());
        
        // Preserve the user association - don't allow changing the user
        // If userId is provided in the update, verify it matches the existing user
        if (financeEntry.getUserId() != null && existingEntry.getUser() != null && 
            !financeEntry.getUserId().equals(existingEntry.getUser().getId())) {
            throw new Exception("Cannot change the user associated with a finance entry");
        }

        return jpaRepo.save(existingEntry);
    }

    @Override
    public List<FinanceEntry> findAll() {
        return jpaRepo.findAll();
    }

    @Override
    public List<FinanceEntry> findByType(String type) {
        return jpaRepo.findByType(type);
    }
    
    @Override
    public List<FinanceEntry> findByUserId(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        // Return empty list if user not found
        return userOpt.map(jpaRepo::findByUser).orElseGet(List::of);
    }
    
    @Override
    public List<FinanceEntry> findByTypeAndUserId(String type, Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        // Return empty list if user not found
        return userOpt.map(user -> jpaRepo.findByTypeAndUser(type, user)).orElseGet(List::of);
    }

    @Override
    public List<Object[]> findCategoryWiseSpendingForCurrentMonth(Long userId) {
        return jpaRepo.findCategoryWiseSpendingForCurrentMonth(userId);
    }

    @Override
    public List<Object[]> findCategoryWiseIncomeForCurrentMonth(Long userId) {
        return jpaRepo.findCategoryWiseIncomeForCurrentMonth(userId);
    }

    @Override
    public FinanceEntry save(FinanceEntry entry) {
        return jpaRepo.save(entry);
    }
}
