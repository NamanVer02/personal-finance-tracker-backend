package com.example.personal_finance_tracker.app.utils;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class CacheMonitoringAspect {

    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object logCacheableOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Cacheable cacheable = signature.getMethod().getAnnotation(Cacheable.class);

        String cacheName = cacheable.value().length > 0 ? cacheable.value()[0] : "default";
        String key = generateKeyString(joinPoint.getArgs());

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - startTime;

        // If execution time is very short, assume it's a cache hit
        if (executionTime < 10) { // 10ms threshold
            log.info("Cache HIT for {}: key={}", cacheName, key);
            LogCollector.addCacheLog(cacheName, "HIT", key);
        } else {
            log.info("Cache MISS for {}: key={}", cacheName, key);
            LogCollector.addCacheLog(cacheName, "MISS", key);
        }

        return result;
    }

    @Around("@annotation(org.springframework.cache.annotation.CacheEvict)")
    public Object logCacheEvictOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        CacheEvict cacheEvict = signature.getMethod().getAnnotation(CacheEvict.class);

        String cacheName = cacheEvict.value().length > 0 ? cacheEvict.value()[0] : "default";
        String key = cacheEvict.allEntries() ? "ALL" : generateKeyString(joinPoint.getArgs());

        log.info("Cache EVICT for {}: key={}", cacheName, key);
        Object result = joinPoint.proceed();
        LogCollector.addCacheLog(cacheName, "EVICTION", key);

        return result;
    }

    @Around("@annotation(org.springframework.cache.annotation.CachePut)")
    public Object logCachePutOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        CachePut cachePut = signature.getMethod().getAnnotation(CachePut.class);

        String cacheName = cachePut.value().length > 0 ? cachePut.value()[0] : "default";
        String key = generateKeyString(joinPoint.getArgs());

        log.info("Cache PUT for {}: key={}", cacheName, key);
        Object result = joinPoint.proceed();
        LogCollector.addCacheLog(cacheName, "PUT", key);

        return result;
    }

    private String generateKeyString(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (Object arg : args) {
            if (arg != null) {
                sb.append(arg.toString()).append(", ");
            } else {
                sb.append("null, ");
            }
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("]");
        return sb.toString();
    }
}
