package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.models.DemoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemoRepository extends JpaRepository<DemoEntity, Long> {
}
