package com.example.personal_finance_tracker.app.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LogCollector {
    private static final CircularFifoQueue<String> logs = new CircularFifoQueue<>(500);
    private static final int MAX_SIZE = 500;

    public static void addLog(String logMessage) {
        // Skip HikariCP internal logs
        if (shouldSkipLog(logMessage)) {
            return;
        }

        logs.add(logMessage);
        // Trim the queue if it gets too large
        while (logs.size() > MAX_SIZE) {
            logs.poll();
        }
    }

    private static boolean shouldSkipLog(String logMessage) {
        // Filter out HikariCP internal logs
        return logMessage.contains("[DevHikariPool]") ||
                logMessage.contains("[HikariPool") ||
                logMessage.contains("[HikariDemo]") ||
                (logMessage.contains("Connection") &&
                        (logMessage.contains("acquired in") ||
                                logMessage.contains("created in") ||
                                logMessage.contains("used for")));
    }

    public static List<String> getLastLogs(int count) {
        List<String> result = new ArrayList<>(logs);
        if (count < result.size()) {
            return result.subList(result.size() - count, result.size());
        }
        return result;
    }

    public static void clearLogs() {
        logs.clear();
    }
}
