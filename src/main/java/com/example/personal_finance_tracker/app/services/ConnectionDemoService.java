package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.utils.LogCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionDemoService {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Transactional(readOnly = true)
    public void demonstrateConnectionReuse() {
        String logMessage = "Starting connection reuse demonstration";
        log.info(logMessage);
        LogCollector.addLog(logMessage);

        // Get current connection from transaction
        Connection connection = DataSourceUtils.getConnection(dataSource);
        String connectionId = getConnectionId(connection);

        // First query
        jdbcTemplate.queryForList("SELECT 1");
        logMessage = "First query executed (Connection #" + connectionId + ")";
        log.info(logMessage);
        LogCollector.addLog(logMessage);

        // Second query (reuses the same connection due to transaction)
        jdbcTemplate.queryForList("SELECT 2");
        logMessage = "Second query executed (Connection #" + connectionId + ") - Demonstrating reuse";
        log.info(logMessage);
        LogCollector.addLog(logMessage);

        logMessage = "Connection reuse demonstration completed";
        log.info(logMessage);
        LogCollector.addLog(logMessage);
    }

    public void demonstrateMultipleConnections(int numberOfQueries) {
        String logMessage = "Starting demonstration with " + numberOfQueries + " concurrent connections";
        log.info(logMessage);
        LogCollector.addLog(logMessage);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfQueries);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfQueries);

        for (int i = 0; i < numberOfQueries; i++) {
            final int queryNumber = i;
            executorService.submit(() -> {
                try {
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    String threadLog = "Thread " + queryNumber + " is preparing to acquire a connection";
                    log.info(threadLog);
                    LogCollector.addLog(threadLog);

                    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                    transactionTemplate.execute(status -> {
                        // Get and log the connection ID for this transaction
                        Connection connection = DataSourceUtils.getConnection(dataSource);
                        String connectionId = getConnectionId(connection);

                        String connLog = "Thread " + queryNumber + " acquired Connection #" + connectionId;
                        log.info(connLog);
                        LogCollector.addLog(connLog);

                        // Execute query
                        jdbcTemplate.queryForList("SELECT " + queryNumber);
                        String queryLog = "Query " + queryNumber + " executing on Connection #" + connectionId;
                        log.info(queryLog);
                        LogCollector.addLog(queryLog);

                        // Hold the connection open for a few seconds
                        try {
                            LogCollector.addLog("Holding Connection #" + connectionId + " open for 5 seconds...");
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        // Execute another query to ensure connection is still active
                        jdbcTemplate.queryForList("SELECT " + queryNumber);
                        String completionLog = "Query " + queryNumber + " completed after delay (still using Connection #" + connectionId + ")";
                        log.info(completionLog);
                        LogCollector.addLog(completionLog);

                        return null;
                    });
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();
        LogCollector.addLog("All threads started simultaneously");

        // Wait for all threads to complete
        try {
            boolean completed = endLatch.await(30, TimeUnit.SECONDS);
            if (completed) {
                String completionLog = "All " + numberOfQueries + " connections completed their work";
                log.info(completionLog);
                LogCollector.addLog(completionLog);
            } else {
                String timeoutLog = "Timed out waiting for all connections to complete";
                log.warn(timeoutLog);
                LogCollector.addLog(timeoutLog);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String interruptLog = "Interrupted while waiting for queries to complete";
            log.error(interruptLog, e);
            LogCollector.addLog(interruptLog + ": " + e.getMessage());
        } finally {
            executorService.shutdown();
            LogCollector.addLog("Connection pool test completed");
        }
    }

    /**
     * Extracts a simplified connection identifier
     * This will extract identifiers like "HikariProxyConnection@1234567" to just "1234567"
     */
    private String getConnectionId(Connection connection) {
        if (connection == null) {
            return "unknown";
        }

        String fullString = connection.toString();

        // For HikariCP connections, the format is typically "HikariProxyConnection@1234567"
        int atIndex = fullString.lastIndexOf('@');
        if (atIndex > 0 && atIndex < fullString.length() - 1) {
            return fullString.substring(atIndex + 1);
        }

        // If we can't parse it in the expected format, return the hashcode
        return String.valueOf(Math.abs(connection.hashCode() % 1000));
    }
}
