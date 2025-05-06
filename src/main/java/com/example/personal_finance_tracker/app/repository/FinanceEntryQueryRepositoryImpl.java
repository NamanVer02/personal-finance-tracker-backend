package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.QFinanceEntry;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FinanceEntryQueryRepositoryImpl implements FinanceEntryQueryRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<FinanceEntry> findFinanceEntriesWithFilters(
            Long userId,
            String type,
            String category,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDate startDate,
            LocalDate endDate,
            String searchTerm,
            Pageable pageable) {

        log.info("Starting filtered query for user ID: {}", userId);
        QFinanceEntry financeEntry = QFinanceEntry.financeEntry;

        JPAQuery<FinanceEntry> query = queryFactory
                .selectFrom(financeEntry)
                .where(financeEntry.user.id.eq(userId));

        // Log filter parameters
        log.debug("Filters - Type: {}, Category: {}, Amount: {}-{}, Date: {}-{}, Search: {}",
                type, category, minAmount, maxAmount, startDate, endDate, searchTerm);

        applyFilters(query, type, category, minAmount, maxAmount, startDate, endDate, searchTerm);
        applySorting(query, pageable);

        long total = query.fetch().size();
        log.debug("Total entries found: {}", total);

        List<FinanceEntry> entries = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        log.info("Returning {} entries for user ID {}", entries.size(), userId);
        return new PageImpl<>(entries, pageable, total);
    }

    @Override
    public Page<FinanceEntry> findAllFinanceEntriesWithFiltersAdmin(
            String type,
            String category,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDate startDate,
            LocalDate endDate,
            String searchTerm,
            Pageable pageable) {

        log.info("Starting admin filtered query");
        QFinanceEntry financeEntry = QFinanceEntry.financeEntry;

        JPAQuery<FinanceEntry> query = queryFactory.selectFrom(financeEntry);

        log.debug("Admin filters - Type: {}, Category: {}, Amount: {}-{}, Date: {}-{}, Search: {}",
                type, category, minAmount, maxAmount, startDate, endDate, searchTerm);

        applyFilters(query, type, category, minAmount, maxAmount, startDate, endDate, searchTerm);
        applySorting(query, pageable);

        long total = query.fetch().size();
        log.debug("Total admin entries found: {}", total);

        List<FinanceEntry> entries = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        log.info("Returning {} admin entries", entries.size());
        return new PageImpl<>(entries, pageable, total);
    }

    private void applyFilters(JPAQuery<FinanceEntry> query,
                              String type,
                              String category,
                              BigDecimal minAmount,
                              BigDecimal maxAmount,
                              LocalDate startDate,
                              LocalDate endDate,
                              String searchTerm) {
        QFinanceEntry financeEntry = QFinanceEntry.financeEntry;

        if (type != null) {
            log.debug("Applying type filter: {}", type);
            query.where(financeEntry.type.eq(type));
        }

        if (category != null && !category.isEmpty()) {
            log.debug("Applying category filter: {}", category);
            query.where(financeEntry.category.in(Arrays.asList(category.split(","))));
        }

        if (minAmount != null) {
            log.debug("Applying minimum amount filter: {}", minAmount);
            query.where(financeEntry.amount.goe(minAmount));
        }

        if (maxAmount != null) {
            log.debug("Applying maximum amount filter: {}", maxAmount);
            query.where(financeEntry.amount.loe(maxAmount));
        }

        if (startDate != null) {
            log.debug("Applying start date filter: {}", startDate);
            query.where(financeEntry.date.goe(startDate));
        }

        if (endDate != null) {
            log.debug("Applying end date filter: {}", endDate);
            query.where(financeEntry.date.loe(endDate));
        }

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            log.debug("Applying search term filter: {}", searchTerm);
            query.where(financeEntry.label.containsIgnoreCase(searchTerm.trim()));
        }
    }

    private void applySorting(JPAQuery<FinanceEntry> query, Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            log.debug("Applying sorting criteria");
            pageable.getSort().forEach(order -> {
                ComparableExpressionBase<?> sortExpression = null;

                switch (order.getProperty()) {
                    case "date":
                        sortExpression = QFinanceEntry.financeEntry.date;
                        break;
                    case "amount":
                        sortExpression = QFinanceEntry.financeEntry.amount;
                        break;
                    case "label":
                        sortExpression = QFinanceEntry.financeEntry.label;
                        break;
                    case "category":
                        sortExpression = QFinanceEntry.financeEntry.category;
                        break;
                    default:
                        log.warn("Unknown sort field: {}", order.getProperty());
                        throw new IllegalArgumentException("Unknown sort field: " + order.getProperty());
                }

                if (sortExpression != null) {
                    log.debug("Sorting by {} {}", order.getProperty(), order.getDirection());
                    query.orderBy(order.isAscending() ? sortExpression.asc() : sortExpression.desc());
                }
            });
        }
    }
}
