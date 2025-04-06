package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, QuerydslPredicateExecutor<Category> {
    Optional<Category> findByName(String name);
    boolean existsByName(String name);
}