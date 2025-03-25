package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.models.DatabaseEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseEventLogRepository extends JpaRepository<DatabaseEventLog, Long> {
}
