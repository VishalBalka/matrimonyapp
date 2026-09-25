package com.matrimonyapp.backend.service;

import com.matrimonyapp.backend.config.AppProperties;
import com.matrimonyapp.backend.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getRateLimit().setEnabled(true);
        rateLimiterService = new RateLimiterService(properties);
    }

    @Test
    void tryAcquire_withinLimit_returnsTrue() {
        String key = "test:ip1";
        assertTrue(rateLimiterService.tryAcquire(key, 3, 60_000));
        assertTrue(rateLimiterService.tryAcquire(key, 3, 60_000));
        assertTrue(rateLimiterService.tryAcquire(key, 3, 60_000));
    }

    @Test
    void tryAcquire_exceedingLimit_returnsFalse() {
        String key = "test:ip2";
        assertTrue(rateLimiterService.tryAcquire(key, 2, 60_000));
        assertTrue(rateLimiterService.tryAcquire(key, 2, 60_000));
        assertFalse(rateLimiterService.tryAcquire(key, 2, 60_000), "Third request must be throttled");
        assertTrue(rateLimiterService.getRetryAfterSeconds(key, 60_000) > 0);
    }
}
