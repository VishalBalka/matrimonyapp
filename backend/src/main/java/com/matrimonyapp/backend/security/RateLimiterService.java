package com.matrimonyapp.backend.security;

import com.matrimonyapp.backend.config.AppProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final AppProperties appProperties;
    private final Map<String, Deque<Long>> requestLogs = new ConcurrentHashMap<>();

    public RateLimiterService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public boolean tryAcquire(String key, int maxRequests, long windowMillis) {
        if (!appProperties.getRateLimit().isEnabled()) {
            return true;
        }

        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;

        Deque<Long> timestamps = requestLogs.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                return true;
            }
            return false;
        }
    }

    public long getRetryAfterSeconds(String key, long windowMillis) {
        Deque<Long> timestamps = requestLogs.get(key);
        if (timestamps == null) {
            return 1;
        }
        synchronized (timestamps) {
            Long earliest = timestamps.peekFirst();
            if (earliest == null) {
                return 1;
            }
            long elapsed = System.currentTimeMillis() - earliest;
            long remaining = (windowMillis - elapsed) / 1000L;
            return Math.max(1, remaining);
        }
    }
}
