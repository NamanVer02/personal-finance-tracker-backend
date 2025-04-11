package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.models.FinanceEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface FinanceEntryQueryRepository {
    Page<FinanceEntry> findFinanceEntriesWithFilters(
            Long userId,
            String type,
            String category,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDate startDate,
            LocalDate endDate,
            String searchTerm,
            Pageable pageable
    );

    Page<FinanceEntry> findAllFinanceEntriesWithFiltersAdmin(
            String type,
            String category,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDate startDate,
            LocalDate endDate,
            String searchTerm,
            Pageable pageable
    );
}