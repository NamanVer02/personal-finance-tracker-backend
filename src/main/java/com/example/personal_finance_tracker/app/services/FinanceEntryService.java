package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.interfaces.FinanceEntryRepoInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceEntryService {
    private final FinanceEntryRepoInterface financeEntryRepo;

    public void deleteById(Long id) {
        log.info("Deleting finance entry with ID: {}", id);
        try {
            financeEntryRepo.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            log.error("Finance entry not found for deletion: ID {}", id, e);
            throw new RuntimeException("Finance entry not found with id: " + id, e);
        } catch (DataAccessException e) {
            log.error("Error deleting finance entry ID: {}", id, e);
            throw new RuntimeException("Failed to delete finance entry", e);
        }
    }

    public FinanceEntry create(FinanceEntry entry) {
        log.info("Creating new finance entry of type: {}", entry.getType());
        try {
            return financeEntryRepo.create(entry);
        } catch (DataAccessException e) {
            log.error("Error creating finance entry of type: {}", entry.getType(), e);
            throw new RuntimeException("Failed to create finance entry", e);
        }
    }

    public FinanceEntry update(Long id, FinanceEntry entry) throws Exception {
        log.info("Updating finance entry ID: {}", id);
        try {
            return financeEntryRepo.update(id, entry);
        } catch (EmptyResultDataAccessException e) {
            log.error("Finance entry not found for update: ID {}", id, e);
            throw new Exception("Finance entry not found with id: " + id);
        } catch (DataAccessException e) {
            log.error("Error updating finance entry ID: {}", id, e);
            throw new RuntimeException("Failed to update finance entry", e);
        }
    }

    public List<FinanceEntry> findAll() {
        log.info("Retrieving all finance entries");
        try {
            return financeEntryRepo.findAll();
        } catch (DataAccessException e) {
            log.error("Error retrieving all finance entries", e);
            throw new RuntimeException("Failed to retrieve finance entries", e);
        }
    }

    public List<FinanceEntry> findByType(String type) {
        log.info("Finding finance entries by type: {}", type);
        try {
            return financeEntryRepo.findByType(type);
        } catch (DataAccessException e) {
            log.error("Error finding finance entries by type: {}", type, e);
            throw new RuntimeException("Failed to find finance entries by type", e);
        }
    }

    public List<FinanceEntry> findByUserId(Long userId) {
        log.info("Finding finance entries for user ID: {}", userId);
        try {
            return financeEntryRepo.findByUserId(userId);
        } catch (DataAccessException e) {
            log.error("Error finding finance entries for user ID: {}", userId, e);
            throw new RuntimeException("Failed to find finance entries by user ID", e);
        }
    }

    public List<FinanceEntry> findByTypeAndUserId(String type, Long userId) {
        log.info("Finding {} entries for user ID: {}", type, userId);
        try {
            return financeEntryRepo.findByTypeAndUserId(type, userId);
        } catch (DataAccessException e) {
            log.error("Error finding {} entries for user ID: {}", type, userId, e);
            throw new RuntimeException("Failed to find finance entries by type and user ID", e);
        }
    }

    public Map<String, Double> getCategoryWiseSpending(Long userId) {
        log.info("Calculating category-wise spending for user ID: {}", userId);
        try {
            List<Object[]> result = financeEntryRepo.findCategoryWiseSpending(userId);
            Map<String, Double> categoryWiseSpending = new HashMap<>();

            for (Object[] row : result) {
                String category = (String) row[0];
                Double totalAmount = (Double) row[1];
                categoryWiseSpending.put(category, totalAmount);
            }
            log.info("Found {} spending categories for user ID: {}", categoryWiseSpending.size(), userId);
            return categoryWiseSpending;
        } catch (DataAccessException e) {
            log.error("Error calculating category-wise spending for user ID: {}", userId, e);
            throw new RuntimeException("Failed to calculate category-wise spending", e);
        }
    }

    public Map<String, Double> getCategoryWiseIncome(Long userId) {
        log.info("Calculating category-wise income for user ID: {}", userId);
        try {
            List<Object[]> result = financeEntryRepo.findCategoryWiseIncome(userId);
            Map<String, Double> categoryWiseIncome = new HashMap<>();

            for (Object[] row : result) {
                String category = (String) row[0];
                Double totalAmount = (Double) row[1];
                categoryWiseIncome.put(category, totalAmount);
            }
            log.info("Found {} income categories for user ID: {}", categoryWiseIncome.size(), userId);
            return categoryWiseIncome;
        } catch (DataAccessException e) {
            log.error("Error calculating category-wise income for user ID: {}", userId, e);
            throw new RuntimeException("Failed to calculate category-wise income", e);
        }
    }

    public double getTotalIncomeForAllUsers() {
        log.info("Calculating total income for all users");
        try {
            return financeEntryRepo.sumByType("Income");
        } catch (DataAccessException e) {
            log.error("Error calculating total income for all users", e);
            throw new RuntimeException("Failed to calculate total income for all users", e);
        }
    }

    public double getTotalExpenseForAllUsers() {
        log.info("Calculating total expense for all users");
        try {
            return financeEntryRepo.sumByType("Expense");
        } catch (DataAccessException e) {
            log.error("Error calculating total expense for all users", e);
            throw new RuntimeException("Failed to calculate total expense for all users", e);
        }
    }

    public double getTotalIncomeForUser(Long userId) {
        log.info("Calculating total income for user ID: {}", userId);
        try {
            return financeEntryRepo.sumByTypeAndUserId("Income", userId);
        } catch (DataAccessException e) {
            log.error("Error calculating total income for user ID: {}", userId, e);
            throw new RuntimeException("Failed to calculate total income for user", e);
        }
    }

    public double getTotalExpenseForUser(Long userId) {
        log.info("Calculating total expense for user ID: {}", userId);
        try {
            return financeEntryRepo.sumByTypeAndUserId("Expense", userId);
        } catch (DataAccessException e) {
            log.error("Error calculating total expense for user ID: {}", userId, e);
            throw new RuntimeException("Failed to calculate total expense for user", e);
        }
    }

    public Map<String, Double> getMonthlyIncomeForAllUsers() {
        log.info("Aggregating monthly income for all users");
        try {
            return financeEntryRepo.getMonthlyAggregateByType("Income");
        } catch (DataAccessException e) {
            log.error("Error aggregating monthly income for all users", e);
            throw new RuntimeException("Failed to get monthly income for all users", e);
        }
    }

    public Map<String, Double> getMonthlyExpenseForAllUsers() {
        log.info("Aggregating monthly expense for all users");
        try {
            return financeEntryRepo.getMonthlyAggregateByType("Expense");
        } catch (DataAccessException e) {
            log.error("Error aggregating monthly expense for all users", e);
            throw new RuntimeException("Failed to get monthly expense for all users", e);
        }
    }

    public Map<String, Double> getCategoryWiseExpenseForCurrentYear(Long userId) {
        log.info("Calculating category-wise annual expenses for user ID: {}", userId);
        try {
            return financeEntryRepo.getCategoryWiseExpenseForCurrentYear(userId);
        } catch (DataAccessException e) {
            log.error("Error calculating category-wise annual expenses for user ID: {}", userId, e);
            throw new RuntimeException("Failed to get category-wise expenses for current year", e);
        }
    }

    public Map<String, Double> getCategoryWiseIncomeForCurrentYear(Long userId) {
        log.info("Calculating category-wise annual income for user ID: {}", userId);
        try {
            return financeEntryRepo.getCategoryWiseIncomeForCurrentYear(userId);
        } catch (DataAccessException e) {
            log.error("Error calculating category-wise annual income for user ID: {}", userId, e);
            throw new RuntimeException("Failed to get category-wise income for current year", e);
        }
    }

    public int getTransactionsCount(Long userId) {
        log.info("Getting transaction count for user ID: {}", userId);
        try {
            return financeEntryRepo.findAllByUser_Id(userId).size();
        } catch (DataAccessException e) {
            log.error("Error getting transaction count for user ID: {}", userId, e);
            throw new RuntimeException("Failed to get transaction count", e);
        }
    }
}
