package com.matrimonyapp.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matrimonyapp.backend.dto.error.ApiErrorResponse;
import com.matrimonyapp.backend.exception.ErrorCode;
import com.matrimonyapp.backend.security.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitingFilter(RateLimiterService rateLimiterService, AppProperties appProperties) {
        this.rateLimiterService = rateLimiterService;
        this.appProperties = appProperties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (!appProperties.getRateLimit().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        int maxRequests = 0;
        long windowMillis = 0;
        String rateLimitKey = null;

        if (path.contains("/api/v1/auth/login")) {
            maxRequests = appProperties.getRateLimit().getLoginRequestsPerFiveMinutes();
            windowMillis = 300_000L; // 5 minutes
            rateLimitKey = "login:" + clientIp;
        } else if (path.contains("/api/v1/auth/register")) {
            maxRequests = appProperties.getRateLimit().getRegisterRequestsPerMinute();
            windowMillis = 60_000L; // 1 minute
            rateLimitKey = "register:" + clientIp;
        } else if (path.contains("/api/v1/auth/otp/")) {
            maxRequests = appProperties.getRateLimit().getOtpRequestsPerTenMinutes();
            windowMillis = 600_000L; // 10 minutes
            rateLimitKey = "otp:" + clientIp;
        }

        if (rateLimitKey != null) {
            if (!rateLimiterService.tryAcquire(rateLimitKey, maxRequests, windowMillis)) {
                long retryAfter = rateLimiterService.getRetryAfterSeconds(rateLimitKey, windowMillis);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", String.valueOf(retryAfter));
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                String requestId = (String) request.getAttribute("X-Request-Id");
                ApiErrorResponse error = new ApiErrorResponse(
                        ErrorCode.RATE_LIMITED.name(),
                        "Too many requests. Please wait " + retryAfter + " seconds before trying again.",
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        path,
                        requestId,
                        null
                );
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
