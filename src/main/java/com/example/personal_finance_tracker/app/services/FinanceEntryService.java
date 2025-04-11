package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.interfaces.FinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.models.FinanceEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        financeEntryRepo.deleteById(id);
    }

    public FinanceEntry create(FinanceEntry entry) {
        log.info("Creating new finance entry of type: {}", entry.getType());
        return financeEntryRepo.create(entry);
    }

    public FinanceEntry update(Long id, FinanceEntry entry) throws Exception {
        log.info("Updating finance entry ID: {}", id);
        return financeEntryRepo.update(id, entry);
    }

    public List<FinanceEntry> findAll() {
        log.info("Retrieving all finance entries");
        return financeEntryRepo.findAll();
    }

    public List<FinanceEntry> findByType(String type) {
        log.info("Finding finance entries by type: {}", type);
        return financeEntryRepo.findByType(type);
    }

    public List<FinanceEntry> findByUserId(Long userId) {
        log.info("Finding finance entries for user ID: {}", userId);
        return financeEntryRepo.findByUserId(userId);
    }

    public List<FinanceEntry> findByTypeAndUserId(String type, Long userId) {
        log.info("Finding {} entries for user ID: {}", type, userId);
        return financeEntryRepo.findByTypeAndUserId(type, userId);
    }

    public Map<String, Double> getCategoryWiseSpending(Long userId) {
        log.info("Calculating category-wise spending for user ID: {}", userId);
        List<Object[]> result = financeEntryRepo.findCategoryWiseSpending(userId);
        Map<String, Double> categoryWiseSpending = new HashMap<>();

        for (Object[] row : result) {
            String category = (String) row[0];
            Double totalAmount = (Double) row[1];
            categoryWiseSpending.put(category, totalAmount);
        }
        log.info("Found {} spending categories for user ID: {}", categoryWiseSpending.size(), userId);
        return categoryWiseSpending;
    }

    public Map<String, Double> getCategoryWiseIncome(Long userId) {
        log.info("Calculating category-wise income for user ID: {}", userId);
        List<Object[]> result = financeEntryRepo.findCategoryWiseIncome(userId);
        Map<String, Double> categoryWiseIncome = new HashMap<>();

        for (Object[] row : result) {
            String category = (String) row[0];
            Double totalAmount = (Double) row[1];
            categoryWiseIncome.put(category, totalAmount);
        }
        log.info("Found {} income categories for user ID: {}", categoryWiseIncome.size(), userId);
        return categoryWiseIncome;
    }

    public double getTotalIncomeForAllUsers() {
        log.info("Calculating total income for all users");
        return financeEntryRepo.sumByType("Income");
    }

    public double getTotalExpenseForAllUsers() {
        log.info("Calculating total expense for all users");
        return financeEntryRepo.sumByType("Expense");
    }

    public double getTotalIncomeForUser(Long userId) {
        log.info("Calculating total income for user ID: {}", userId);
        return financeEntryRepo.sumByTypeAndUserId("Income", userId);
    }

    public double getTotalExpenseForUser(Long userId) {
        log.info("Calculating total expense for user ID: {}", userId);
        return financeEntryRepo.sumByTypeAndUserId("Expense", userId);
    }

    public Map<String, Double> getMonthlyIncomeForAllUsers() {
        log.info("Aggregating monthly income for all users");
        return financeEntryRepo.getMonthlyAggregateByType("Income");
    }

    public Map<String, Double> getMonthlyExpenseForAllUsers() {
        log.info("Aggregating monthly expense for all users");
        return financeEntryRepo.getMonthlyAggregateByType("Expense");
    }

    public Map<String, Double> getCategoryWiseExpenseForCurrentYear(Long userId) {
        log.info("Calculating category-wise annual expenses for user ID: {}", userId);
        return financeEntryRepo.getCategoryWiseExpenseForCurrentYear(userId);
    }

    public Map<String, Double> getCategoryWiseIncomeForCurrentYear(Long userId) {
        log.info("Calculating category-wise annual income for user ID: {}", userId);
        return financeEntryRepo.getCategoryWiseIncomeForCurrentYear(userId);
    }

    public int getTransactionsCount(Long userId) {
        log.info("Getting transaction count for user ID: {}", userId);
        return financeEntryRepo.findAllByUser_Id(userId).size();
    }
}
