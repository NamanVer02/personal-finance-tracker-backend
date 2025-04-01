package com.example.personal_finance_tracker.app.interfaces;

import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaFinanceEntryRepoInterface extends JpaRepository<FinanceEntry, Long>, QuerydslPredicateExecutor<FinanceEntry> {
    List<FinanceEntry> findByType(String type);
    List<FinanceEntry> findByUser(User user);
    List<FinanceEntry> findByTypeAndUser(String type, User user);


    @Query("SELECT f.category, SUM(f.amount) FROM FinanceEntry f " +
            "WHERE f.user.id = :userId AND f.type = 'Expense' AND YEAR(f.date) = YEAR(CURRENT_DATE) AND MONTH(f.date) = MONTH(CURRENT_DATE) " +
            "GROUP BY f.category")
    List<Object[]> findCategoryWiseSpendingForCurrentMonth(@Param("userId") Long userId);

    @Query("SELECT f.category, SUM(f.amount) FROM FinanceEntry f " +
            "WHERE f.user.id = :userId AND f.type = 'Income' AND YEAR(f.date) = YEAR(CURRENT_DATE) AND MONTH(f.date) = MONTH(CURRENT_DATE) " +
            "GROUP BY f.category")
    List<Object[]> findCategoryWiseIncomeForCurrentMonth(@Param("userId") Long userId);

    @Query("SELECT f.category, SUM(f.amount) FROM FinanceEntry f " +
            "WHERE f.user.id = :userId AND f.type = 'Expense' AND YEAR(f.date) = YEAR(CURRENT_DATE) " +
            "GROUP BY f.category")
    List<Object[]> findCategoryWiseSpendingForCurrentYear(@Param("userId") Long userId);

    @Query("SELECT f.category, SUM(f.amount) FROM FinanceEntry f " +
            "WHERE f.user.id = :userId AND f.type = 'Income' AND YEAR(f.date) = YEAR(CURRENT_DATE) " +
            "GROUP BY f.category")
    List<Object[]> findCategoryWiseIncomeForCurrentYear(@Param("userId") Long userId);
}
