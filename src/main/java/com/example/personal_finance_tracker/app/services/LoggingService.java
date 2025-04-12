package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.models.DatabaseEventLog;
import com.example.personal_finance_tracker.app.repository.DatabaseEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingService {

    private final DatabaseEventLogRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDatabaseEvent(String user, String method, String entityName, String entityId, String description) {
        try {
            DatabaseEventLog eventLog = DatabaseEventLog.builder()
                    .date(LocalDate.now())
                    .time(LocalTime.now())
                    .username(user)
                    .method(method)
                    .entityName(entityName)
                    .entityId(entityId)
                    .description(description)
                    .build();

            logRepository.save(eventLog);
        } catch (DataAccessException e) {
            // We don't want to throw an exception here as it might disrupt the main operation
            // Just log the error and continue
            log.error("Failed to log database event: user={}, method={}, entity={}, id={}", 
                    user, method, entityName, entityId, e);
        } catch (Exception e) {
            log.error("Unexpected error while logging database event", e);
        }
    }
}
