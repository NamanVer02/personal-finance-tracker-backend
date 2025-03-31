package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.interfaces.FinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.models.FinanceEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Map<String, Double> getCategoryWiseSpendingForCurrentMonth(Long userId) {
        List<Object[]> result = financeEntryRepo.findCategoryWiseSpendingForCurrentMonth(userId);
        Map<String, Double> categoryWiseSpending = new HashMap<>();

        for (Object[] row : result) {
            String category = (String) row[0];
            Double totalAmount = (Double) row[1];
            categoryWiseSpending.put(category, totalAmount);
        }

        return categoryWiseSpending;
    }

    public Map<String, Double> getCategoryWiseIncomeForCurrentMonth(Long userId) {
        List<Object[]> result = financeEntryRepo.findCategoryWiseIncomeForCurrentMonth(userId);
        Map<String, Double> categoryWiseIncome = new HashMap<>();

        for (Object[] row : result) {
            String category = (String) row[0];
            Double totalAmount = (Double) row[1];
            categoryWiseIncome.put(category, totalAmount);
        }

        return categoryWiseIncome;
    }

    public double getTotalIncomeForAllUsers() {
        // Implementation to calculate total income for all users
        return financeEntryRepo.sumByType("income");
    }

    public double getTotalExpenseForAllUsers() {
        // Implementation to calculate total expense for all users
        return financeEntryRepo.sumByType("expense");
    }

    public double getTotalIncomeForUser(Long userId) {
        // Implementation to calculate total income for a specific user
        return financeEntryRepo.sumByTypeAndUserId("income", userId);
    }

    public double getTotalExpenseForUser(Long userId) {
        // Implementation to calculate total expense for a specific user
        return financeEntryRepo.sumByTypeAndUserId("expense", userId);
    }

    public Map<String, Double> getMonthlyIncomeForAllUsers() {
        // Implementation to get monthly income aggregated by month
        return financeEntryRepo.getMonthlyAggregateByType("income");
    }

    public Map<String, Double> getMonthlyExpenseForAllUsers() {
        // Implementation to get monthly expense aggregated by month
        return financeEntryRepo.getMonthlyAggregateByType("expense");
    }

    public Map<String, Double> getCategoryWiseExpenseForCurrentMonth(Long userId) {
        // Implementation to get category-wise expense for current month
        return financeEntryRepo.getCategoryWiseExpenseForCurrentMonth(userId);
    }
}
