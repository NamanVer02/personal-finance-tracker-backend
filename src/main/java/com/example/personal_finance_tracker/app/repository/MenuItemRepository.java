package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.models.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findAllByIsActiveTrue();
    List<MenuItem> findAllByIsActiveTrueOrderByDisplayOrderAsc();
}