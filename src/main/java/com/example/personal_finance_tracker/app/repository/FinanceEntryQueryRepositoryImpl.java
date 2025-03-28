package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.QFinanceEntry;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
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

        QFinanceEntry financeEntry = QFinanceEntry.financeEntry;
        
        JPAQuery<FinanceEntry> query = queryFactory
            .selectFrom(financeEntry)
            .where(financeEntry.user.id.eq(userId));

        // Add dynamic filters
        if (type != null) {
            query.where(financeEntry.type.eq(type));
        }
        
        if (category != null) {
            query.where(financeEntry.category.eq(category));
        }
        
        if (minAmount != null) {
            query.where(financeEntry.amount.goe(minAmount));
        }
        
        if (maxAmount != null) {
            query.where(financeEntry.amount.loe(maxAmount));
        }
        
        if (startDate != null) {
            query.where(financeEntry.date.goe(startDate));
        }
        
        if (endDate != null) {
            query.where(financeEntry.date.loe(endDate));
        }

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            query.where(financeEntry.category.containsIgnoreCase(searchTerm));
        }

        // Handle sorting
        if (pageable.getSort().isSorted()) {
            PathBuilder<FinanceEntry> pathBuilder = new PathBuilder<>(FinanceEntry.class, "financeEntry");
            pageable.getSort().forEach(order -> {
                query.orderBy(order.isAscending() ? 
                    pathBuilder.getString(order.getProperty()).asc() : 
                    pathBuilder.getString(order.getProperty()).desc());
            });
        }

        // Get total count for pagination
        long total = query.fetchCount();

        // Apply pagination
        List<FinanceEntry> entries = query
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        return new PageImpl<>(entries, pageable, total);
    }
}