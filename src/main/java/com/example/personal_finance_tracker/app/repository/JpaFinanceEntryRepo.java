package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.exceptions.JwtAuthenticationException;
import com.example.personal_finance_tracker.app.interfaces.FinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.interfaces.JpaFinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JpaFinanceEntryRepo implements FinanceEntryRepoInterface {
    private final JpaFinanceEntryRepoInterface jpaRepo;
    private final UserRepo userRepo;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void deleteById(Long id) {
        log.info("Deleting finance entry with ID: {}", id);
        jpaRepo.deleteById(id);
        log.debug("Successfully deleted finance entry ID: {}", id);
    }

    @Override
    public FinanceEntry create(FinanceEntry entry) {
        log.info("Creating new finance entry for user ID: {}", entry.getUserId());
        if (entry.getUserId() != null) {
            userRepo.findById(entry.getUserId()).ifPresent(user -> {
                entry.setUser(user);
                log.debug("Associated user found: {}", user.getUsername());
            });
        }
        FinanceEntry createdEntry = jpaRepo.save(entry);
        log.info("Created new finance entry ID: {}", createdEntry.getId());
        return createdEntry;
    }

    @Override
    public FinanceEntry update(Long id, FinanceEntry financeEntry) throws Exception {
        log.info("Updating finance entry ID: {}", id);
        FinanceEntry existingEntry = jpaRepo.findById(id)
                .orElseThrow(() -> {
                    log.error("Entry not found for ID: {}", id);
                    return new Exception("No entry found with the id" + id);
                });

        log.debug("Updating entry fields for ID: {}", id);
        existingEntry.setLabel(financeEntry.getLabel());
        existingEntry.setType(financeEntry.getType());
        existingEntry.setAmount(financeEntry.getAmount());
        existingEntry.setCategory(financeEntry.getCategory());
        existingEntry.setDate(financeEntry.getDate());

        if (financeEntry.getUserId() != null && existingEntry.getUser() != null &&
                !financeEntry.getUserId().equals(existingEntry.getUser().getId())) {
            log.warn("Attempt to change user association for entry ID: {}", id);
            throw new JwtAuthenticationException("Cannot change the user associated with a finance entry");
        }

        FinanceEntry updatedEntry = jpaRepo.save(existingEntry);
        log.info("Successfully updated entry ID: {}", updatedEntry.getId());
        return updatedEntry;
    }

    @Override
    public List<FinanceEntry> findAll() {
        log.info("Fetching all finance entries");
        List<FinanceEntry> entries = jpaRepo.findAll();
        log.debug("Retrieved {} entries", entries.size());
        return entries;
    }

    @Override
    public List<FinanceEntry> findByType(String type) {
        log.info("Fetching entries by type: {}", type);
        List<FinanceEntry> entries = jpaRepo.findByType(type);
        log.debug("Found {} entries of type {}", entries.size(), type);
        return entries;
    }

    @Override
    public List<FinanceEntry> findByUserId(Long userId) {
        log.info("Fetching entries for user ID: {}", userId);
        Optional<User> userOpt = userRepo.findById(userId);
        List<FinanceEntry> entries = userOpt.map(jpaRepo::findByUser)
                .orElseGet(() -> {
                    log.warn("User ID {} not found", userId);
                    return List.of();
                });
        log.debug("Retrieved {} entries for user ID {}", entries.size(), userId);
        return entries;
    }

    @Override
    public List<FinanceEntry> findByTypeAndUserId(String type, Long userId) {
        log.info("Fetching {} entries for user ID: {}", type, userId);
        Optional<User> userOpt = userRepo.findById(userId);
        List<FinanceEntry> entries = userOpt.map(user -> jpaRepo.findByTypeAndUser(type, user))
                .orElseGet(() -> {
                    log.warn("User ID {} not found", userId);
                    return List.of();
                });
        log.debug("Found {} {} entries for user ID {}", entries.size(), type, userId);
        return entries;
    }

    @Override
    public List<Object[]> findCategoryWiseSpending(Long userId) {
        log.info("Fetching category-wise spending for user ID: {}", userId);
        List<Object[]> results = jpaRepo.findCategoryWiseSpending(userId);
        log.debug("Retrieved {} spending categories for user ID {}", results.size(), userId);
        return results;
    }

    @Override
    public List<Object[]> findCategoryWiseIncome(Long userId) {
        log.info("Fetching category-wise income for user ID: {}", userId);
        List<Object[]> results = jpaRepo.findCategoryWiseIncome(userId);
        log.debug("Retrieved {} income categories for user ID {}", results.size(), userId);
        return results;
    }

    @Override
    public FinanceEntry save(FinanceEntry entry) {
        log.info("Saving finance entry ID: {}", entry.getId());
        FinanceEntry savedEntry = jpaRepo.save(entry);
        log.debug("Successfully saved entry ID: {}", savedEntry.getId());
        return savedEntry;
    }

    @Override
    public double sumByType(String type) {
        log.info("Calculating total {} amount", type);
        List<FinanceEntry> entries = jpaRepo.findByType(type);
        double total = entries.stream()
                .mapToDouble(FinanceEntry::getAmount)
                .sum();
        log.debug("Total {} amount: {}", type, total);
        return total;
    }

    @Override
    public double sumByTypeAndUserId(String type, Long userId) {
        log.info("Calculating {} total for user ID: {}", type, userId);
        Optional<User> userOptional = userRepo.findById(userId);
        double total = userOptional.map(user -> {
            List<FinanceEntry> entries = jpaRepo.findByTypeAndUser(type, user);
            return entries.stream()
                    .mapToDouble(FinanceEntry::getAmount)
                    .sum();
        }).orElse(0.0);
        log.debug("User ID {} {} total: {}", userId, type, total);
        return total;
    }

    @Override
    public Map<String, Double> getMonthlyAggregateByType(String type) {
        log.info("Generating monthly aggregates for {}", type);
        Map<String, Double> monthlyAggregates = new HashMap<>();
        List<FinanceEntry> entries = jpaRepo.findByType(type);

        entries.forEach(entry -> {
            LocalDate date = entry.getDate();
            String monthYear = date.getMonth().toString() + " " + date.getYear();
            monthlyAggregates.merge(monthYear, entry.getAmount(), Double::sum);
        });

        log.debug("Monthly aggregates calculated for {} months", monthlyAggregates.size());
        return monthlyAggregates;
    }

    @Override
    public Map<String, Double> getCategoryWiseExpenseForCurrentYear(Long userId) {
        log.info("Fetching category-wise expenses for user ID: {}", userId);
        List<Object[]> results = jpaRepo.findCategoryWiseSpendingForCurrentYear(userId);
        Map<String, Double> categoryWiseExpense = new HashMap<>();

        for (Object[] row : results) {
            String category = (String) row[0];
            Double amount = (Double) row[1];
            categoryWiseExpense.put(category, amount);
        }

        log.debug("Retrieved {} expense categories for user ID {}", categoryWiseExpense.size(), userId);
        return categoryWiseExpense;
    }

    @Override
    public Map<String, Double> getCategoryWiseIncomeForCurrentYear(Long userId) {
        log.info("Fetching category-wise income for user ID: {}", userId);
        List<Object[]> results = jpaRepo.findCategoryWiseIncomeForCurrentYear(userId);
        Map<String, Double> categoryWiseExpense = new HashMap<>();

        for (Object[] row : results) {
            String category = (String) row[0];
            Double amount = (Double) row[1];
            categoryWiseExpense.put(category, amount);
        }

        log.debug("Retrieved {} income categories for user ID {}", categoryWiseExpense.size(), userId);
        return categoryWiseExpense;
    }

    @Transactional
    @Override
    public List<FinanceEntry> saveAll(List<FinanceEntry> entries) {
        log.info("Bulk saving {} entries", entries.size());
        for (int i = 0; i < entries.size(); i++) {
            entityManager.persist(entries.get(i));
            if (i % 100 == 0) {
                entityManager.flush();
                entityManager.clear();
                log.debug("Flushed batch at index {}", i);
            }
        }
        entityManager.flush();
        entityManager.clear();
        log.info("Completed bulk save operation");
        return entries;
    }

    @Override
    public List<FinanceEntry> findAllByUser_Id(Long id) {
        log.info("Finding all entries for user ID: {}", id);
        List<FinanceEntry> entries = jpaRepo.findByUser_Id(id);
        log.debug("Found {} entries for user ID {}", entries.size(), id);
        return entries;
    }
}
