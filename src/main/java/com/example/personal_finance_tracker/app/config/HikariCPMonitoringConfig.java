package com.example.personal_finance_tracker.app.config;

import com.example.personal_finance_tracker.app.utils.LogCollector;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class HikariCPMonitoringConfig {

    @Bean
    public DataSourceHealthIndicator dataSourceHealthIndicator(DataSource dataSource) {
        return new DataSourceHealthIndicator(dataSource, "SELECT 1");
    }

    @Autowired
    public void configureHikariLogMetrics(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            // Fix: Use setMetricsTrackerFactory instead of setMetricRegistry
            hikariDataSource.setMetricsTrackerFactory(new LoggingMetricsTracker());
        }
    }

    // Custom metrics tracker that logs connection events
    @Slf4j
    public static class LoggingMetricsTracker implements MetricsTrackerFactory {
        @Override
        public IMetricsTracker create(String poolName, PoolStats poolStats) {
            return new LogTracker(poolName);
        }

        @Slf4j
        private static class LogTracker implements IMetricsTracker {
            private final String poolName;

            public LogTracker(String poolName) {
                this.poolName = poolName;
            }

            @Override
            public void recordConnectionAcquiredNanos(long elapsedAcquiredNanos) {
                String message = String.format("[%s] Connection acquired in %dms", poolName, elapsedAcquiredNanos/1000000);
                log.info(message);
                LogCollector.addLog(message);
            }


            @Override
            public void recordConnectionUsageMillis(long elapsedBorrowedMillis) {
                log.info("[{}] Connection used for {}ms", poolName, elapsedBorrowedMillis);
            }

            @Override
            public void recordConnectionCreatedMillis(long connectionCreatedMillis) {
                log.info("[{}] Connection created in {}ms", poolName, connectionCreatedMillis);
            }

            @Override
            public void recordConnectionTimeout() {
                log.warn("[{}] Connection timeout occurred", poolName);
            }
        }
    }
}
