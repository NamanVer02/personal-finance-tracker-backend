package com.example.personal_finance_tracker.app.interfaces;

import com.example.personal_finance_tracker.app.models.JpaFinanceEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaFinanceEntryRepoInterface extends JpaRepository<JpaFinanceEntry, Long> {
    List<JpaFinanceEntry> findByType (String type);
}
