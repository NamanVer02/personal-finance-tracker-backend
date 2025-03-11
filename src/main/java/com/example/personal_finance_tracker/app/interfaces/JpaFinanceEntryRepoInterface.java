package com.example.personal_finance_tracker.app.interfaces;

import com.example.personal_finance_tracker.app.models.JpaFinanceEntry;
import com.example.personal_finance_tracker.app.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaFinanceEntryRepoInterface extends JpaRepository<JpaFinanceEntry, Long> {
    List<JpaFinanceEntry> findByType(String type);
    List<JpaFinanceEntry> findByUser(User user);
    List<JpaFinanceEntry> findByTypeAndUser(String type, User user);

    @Query("SELECT f.category, SUM(f.amount) FROM JpaFinanceEntry f " +
            "WHERE f.user.id = :userId AND f.type = 'Expense' AND YEAR(f.date) = YEAR(CURRENT_DATE) AND MONTH(f.date) = MONTH(CURRENT_DATE) " +
            "GROUP BY f.category")
    List<Object[]> findCategoryWiseSpendingForCurrentMonth(@Param("userId") Long userId);

    @Query("SELECT f.category, SUM(f.amount) FROM JpaFinanceEntry f " +
            "WHERE f.user.id = :userId AND f.type = 'Income' AND YEAR(f.date) = YEAR(CURRENT_DATE) AND MONTH(f.date) = MONTH(CURRENT_DATE) " +
            "GROUP BY f.category")
    List<Object[]> findCategoryWiseIncomeForCurrentMonth(Long userId);
}
