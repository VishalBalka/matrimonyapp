package com.matrimonyapp.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Otp otp = new Otp();
    private RateLimit rateLimit = new RateLimit();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Otp getOtp() {
        return otp;
    }

    public void setOtp(Otp otp) {
        this.otp = otp;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public static class Jwt {
        private String secret = "9a4f2c8d7e1b5a3f6c8d0e2b4a6c8e0f1a3b5c7d9e1f3a5b7c9d1e3f5a7b9c1d";
        private long accessTokenExpirationMs = 86400000L; // 24 hours
        private long refreshTokenExpirationMs = 604800000L; // 7 days

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessTokenExpirationMs() {
            return accessTokenExpirationMs;
        }

        public void setAccessTokenExpirationMs(long accessTokenExpirationMs) {
            this.accessTokenExpirationMs = accessTokenExpirationMs;
        }

        public long getRefreshTokenExpirationMs() {
            return refreshTokenExpirationMs;
        }

        public void setRefreshTokenExpirationMs(long refreshTokenExpirationMs) {
            this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        }
    }

    public static class Otp {
        private int expirationMinutes = 5;
        private int maxAttempts = 3;
        private int cooldownSeconds = 60;

        public int getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(int expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getCooldownSeconds() {
            return cooldownSeconds;
        }

        public void setCooldownSeconds(int cooldownSeconds) {
            this.cooldownSeconds = cooldownSeconds;
        }
    }

    public static class RateLimit {
        private boolean enabled = true;
        private int loginRequestsPerFiveMinutes = 10;
        private int registerRequestsPerMinute = 5;
        private int otpRequestsPerTenMinutes = 3;
        private int apiRequestsPerMinute = 120;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getLoginRequestsPerFiveMinutes() {
            return loginRequestsPerFiveMinutes;
        }

        public void setLoginRequestsPerFiveMinutes(int loginRequestsPerFiveMinutes) {
            this.loginRequestsPerFiveMinutes = loginRequestsPerFiveMinutes;
        }

        public int getRegisterRequestsPerMinute() {
            return registerRequestsPerMinute;
        }

        public void setRegisterRequestsPerMinute(int registerRequestsPerMinute) {
            this.registerRequestsPerMinute = registerRequestsPerMinute;
        }

        public int getOtpRequestsPerTenMinutes() {
            return otpRequestsPerTenMinutes;
        }

        public void setOtpRequestsPerTenMinutes(int otpRequestsPerTenMinutes) {
            this.otpRequestsPerTenMinutes = otpRequestsPerTenMinutes;
        }

        public int getApiRequestsPerMinute() {
            return apiRequestsPerMinute;
        }

        public void setApiRequestsPerMinute(int apiRequestsPerMinute) {
            this.apiRequestsPerMinute = apiRequestsPerMinute;
        }
    }
}
