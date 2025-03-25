package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.models.DatabaseEventLog;
import com.example.personal_finance_tracker.app.repository.DatabaseEventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;


@Service
@RequiredArgsConstructor
public class LoggingService {

    private final DatabaseEventLogRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDatabaseEvent(String user, String method, String entityName, String entityId, String description) {
        DatabaseEventLog log = DatabaseEventLog.builder()
                .date(LocalDate.now())
                .time(LocalTime.now())
                .username(user)
                .method(method)
                .entityName(entityName)
                .entityId(entityId)
                .description(description)
                .build();

        logRepository.save(log);
    }
}
