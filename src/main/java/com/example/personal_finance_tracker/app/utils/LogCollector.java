package com.example.personal_finance_tracker.app.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class LogCollector {
    private static final CircularFifoQueue<String> logs = new CircularFifoQueue<>(500);
    private static final CircularFifoQueue<String> cacheLogs = new CircularFifoQueue<>(500);
    private static final int MAX_SIZE = 500;

    // Cache statistics
    private static final Map<String, AtomicLong> cacheHits = new HashMap<>();
    private static final Map<String, AtomicLong> cacheMisses = new HashMap<>();
    private static final Map<String, AtomicLong> cacheEvictions = new HashMap<>();

    // History for charts
    private static final List<String> timestamps = new ArrayList<>();
    private static final Map<String, List<Double>> hitRates = new HashMap<>();
    private static final int MAX_HISTORY_SIZE = 30;

    private LogCollector() {
        // Prevents instantiation
    }

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

    public static void addCacheLog(String cacheName, String operation, String key) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        String logMessage = String.format("[%s] [%s] %s - key: %s", timestamp, cacheName, operation, key);
        cacheLogs.add(logMessage);

        // Update statistics
        if ("HIT".equals(operation)) {
            cacheHits.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
        } else if ("MISS".equals(operation)) {
            cacheMisses.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
        } else if ("EVICTION".equals(operation)) {
            cacheEvictions.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
        }

        // Update history periodically
        updateMetricHistory();
    }

    private static void updateMetricHistory() {
        if (timestamps.size() >= MAX_HISTORY_SIZE) {
            timestamps.remove(0);
            for (List<Double> rates : hitRates.values()) {
                if (!rates.isEmpty()) {
                    rates.remove(0);
                }
            }
        }

        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
        timestamps.add(timestamp);

        // Calculate hit rates for all caches
        for (String cacheName : cacheHits.keySet()) {
            long hits = cacheHits.getOrDefault(cacheName, new AtomicLong(0)).get();
            long misses = cacheMisses.getOrDefault(cacheName, new AtomicLong(0)).get();
            double hitRate = hits + misses > 0 ? (double) hits / (hits + misses) * 100 : 0;

            hitRates.computeIfAbsent(cacheName, k -> new ArrayList<>()).add(hitRate);
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

    public static List<String> getCacheLogs(int count) {
        List<String> result = new ArrayList<>(cacheLogs);
        if (count < result.size()) {
            return result.subList(result.size() - count, result.size());
        }
        return result;
    }

    public static Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        // Overall statistics
        long totalHits = cacheHits.values().stream().mapToLong(AtomicLong::get).sum();
        long totalMisses = cacheMisses.values().stream().mapToLong(AtomicLong::get).sum();
        long totalEvictions = cacheEvictions.values().stream().mapToLong(AtomicLong::get).sum();
        double overallHitRate = totalHits + totalMisses > 0 ?
                (double) totalHits / (totalHits + totalMisses) * 100 : 0;

        stats.put("totalHits", totalHits);
        stats.put("totalMisses", totalMisses);
        stats.put("totalEvictions", totalEvictions);
        stats.put("hitRate", overallHitRate);

        // Per-cache statistics
        Map<String, Map<String, Object>> cachesStats = new HashMap<>();
        for (String cacheName : cacheHits.keySet()) {
            Map<String, Object> cacheStats = new HashMap<>();
            long hits = cacheHits.getOrDefault(cacheName, new AtomicLong(0)).get();
            long misses = cacheMisses.getOrDefault(cacheName, new AtomicLong(0)).get();
            long evictions = cacheEvictions.getOrDefault(cacheName, new AtomicLong(0)).get();
            double hitRate = hits + misses > 0 ? (double) hits / (hits + misses) * 100 : 0;

            cacheStats.put("hits", hits);
            cacheStats.put("misses", misses);
            cacheStats.put("evictions", evictions);
            cacheStats.put("hitRate", hitRate);

            cachesStats.put(cacheName, cacheStats);
        }
        stats.put("caches", cachesStats);

        // Time series data for charts
        stats.put("timestamps", timestamps);
        stats.put("hitRates", hitRates);

        return stats;
    }

    public static void clearLogs() {
        logs.clear();
    }

    public static void clearCacheLogs() {
        cacheLogs.clear();
        cacheHits.clear();
        cacheMisses.clear();
        cacheEvictions.clear();
        timestamps.clear();
        hitRates.clear();
    }
}
