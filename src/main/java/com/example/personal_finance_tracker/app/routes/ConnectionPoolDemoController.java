package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.services.ConnectionDemoService;
import com.example.personal_finance_tracker.app.utils.LogCollector;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class ConnectionPoolDemoController {

    private final ConnectionDemoService demoService;
    private final DataSource dataSource;

    @GetMapping("/connection-reuse")
    public Map<String, String> demonstrateConnectionReuse() {
        log.info("Starting connection reuse demonstration");
        demoService.demonstrateConnectionReuse();
        return Collections.singletonMap("status", "Connection reuse demonstration completed");
    }

    @GetMapping("/multiple-connections")
    public Map<String, String> demonstrateMultipleConnections(@RequestParam(defaultValue = "15") int count) {
        log.info("Starting multiple connections demonstration with {} queries", count);
        demoService.demonstrateMultipleConnections(count);
        return Collections.singletonMap("status", "Multiple connections demonstration completed with " + count + " queries");
    }

    @GetMapping("/pool-stats")
    public Map<String, Object> getPoolStats() {
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeConnections", poolMXBean.getActiveConnections());
        stats.put("idleConnections", poolMXBean.getIdleConnections());
        stats.put("totalConnections", poolMXBean.getTotalConnections());
        stats.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());

        log.debug("Current pool stats: {}", stats);
        return stats;
    }

    @GetMapping("/connection-logs")
    public List<String> getConnectionLogs() {
        return LogCollector.getLastLogs(100); // Return last 100 logs
    }

    @GetMapping("/reset")
    public Map<String, String> resetDemo() {
        LogCollector.clearLogs();
        log.info("Connection pool demo reset");
        return Collections.singletonMap("status", "Demo reset successfully");
    }
}
