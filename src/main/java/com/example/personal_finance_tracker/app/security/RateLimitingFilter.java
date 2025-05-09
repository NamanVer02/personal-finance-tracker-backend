package com.example.personal_finance_tracker.app.security;

import org.springframework.stereotype.Component;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter implements Filter {
    private static final int MAX_REQUESTS_PER_MINUTE = 2;
    private static final long TIME_WINDOW_MS = TimeUnit.MINUTES.toMillis(1);
    private final Map<String, RequestCounter> ipRequestCounts = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();
        if ("/api/auth/forgot-password".equals(path)) {
            String ip = getClientIp(httpRequest);
            long now = System.currentTimeMillis();
            ipRequestCounts.compute(ip, (key, counter) -> {
                if (counter == null || now - counter.startTime > TIME_WINDOW_MS) {
                    return new RequestCounter(1, now);
                } else {
                    counter.count++;
                    return counter;
                }
            });
            RequestCounter counter = ipRequestCounts.get(ip);
            if (counter.count > MAX_REQUESTS_PER_MINUTE) {
                httpResponse.setStatus(429);
                httpResponse.getWriter().write("Rate limit exceeded. Try again later.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private static class RequestCounter {
        int count;
        long startTime;
        RequestCounter(int count, long startTime) {
            this.count = count;
            this.startTime = startTime;
        }
    }
}