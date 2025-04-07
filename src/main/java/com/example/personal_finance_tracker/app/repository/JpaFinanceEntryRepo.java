package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.interfaces.FinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.interfaces.JpaFinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaFinanceEntryRepo implements FinanceEntryRepoInterface {
    private final JpaFinanceEntryRepoInterface jpaRepo;
    private final UserRepo userRepo;

    @PersistenceContext
    private EntityManager entityManager;

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

    @Override
    public double sumByType(String type) {
        // Calculate sum of amounts for a given type (income or expense)
        List<FinanceEntry> entries = jpaRepo.findByType(type);
        return entries.stream()
                .mapToDouble(FinanceEntry::getAmount)
                .sum();
    }

    @Override
    public double sumByTypeAndUserId(String type, Long userId) {
        // Calculate sum of amounts for a given type and user
        Optional<User> userOptional = userRepo.findById(userId);
        if (userOptional.isPresent()) {
            List<FinanceEntry> entries = jpaRepo.findByTypeAndUser(type, userOptional.get());
            return entries.stream()
                    .mapToDouble(FinanceEntry::getAmount)
                    .sum();
        }
        return 0.0;
    }

    @Override
    public Map<String, Double> getMonthlyAggregateByType(String type) {
        // Calculate monthly aggregates for a given type
        Map<String, Double> monthlyAggregates = new HashMap<>();
        List<FinanceEntry> entries = jpaRepo.findByType(type);

        // Group entries by month and sum amounts
        entries.forEach(entry -> {
            LocalDate date = entry.getDate();
            String monthYear = date.getMonth().toString() + " " + date.getYear();

            monthlyAggregates.merge(monthYear, entry.getAmount(), Double::sum);
        });

        return monthlyAggregates;
    }

    @Override
    public Map<String, Double> getCategoryWiseExpenseForCurrentYear(Long userId) {
        List<Object[]> results = jpaRepo.findCategoryWiseSpendingForCurrentYear(userId);
        Map<String, Double> categoryWiseExpense = new HashMap<>();

        for (Object[] row : results) {
            String category = (String) row[0];
            Double amount = (Double) row[1];
            categoryWiseExpense.put(category, amount);
        }

        return categoryWiseExpense;
    }

    @Override
    public Map<String, Double> getCategoryWiseIncomeForCurrentYear(Long userId) {
        List<Object[]> results = jpaRepo.findCategoryWiseIncomeForCurrentYear(userId);
        Map<String, Double> categoryWiseExpense = new HashMap<>();

        for (Object[] row : results) {
            String category = (String) row[0];
            Double amount = (Double) row[1];
            categoryWiseExpense.put(category, amount);
        }

        return categoryWiseExpense;
    }

    @Transactional
    @Override
    public List<FinanceEntry> saveAll(List<FinanceEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            entityManager.persist(entries.get(i));
            if (i % 100 == 0) { // Flush every 100 entries
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
        return entries;
    }
}
