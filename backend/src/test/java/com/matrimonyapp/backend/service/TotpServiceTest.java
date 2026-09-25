package com.matrimonyapp.backend.service;

import com.matrimonyapp.backend.security.TotpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TotpServiceTest {

    private TotpService totpService;

    @BeforeEach
    void setUp() {
        totpService = new TotpService();
    }

    @Test
    void generateSecret_returnsValidBase32String() {
        String secret = totpService.generateSecret();
        assertNotNull(secret);
        assertTrue(secret.matches("^[A-Z2-7]+$"), "Secret must be valid Base32 characters");
        assertTrue(secret.length() >= 16);
    }

    @Test
    void verifyCode_invalidCode_returnsFalse() {
        String secret = totpService.generateSecret();
        assertFalse(totpService.verifyCode(secret, "000000"));
        assertFalse(totpService.verifyCode(secret, "abc"));
        assertFalse(totpService.verifyCode(secret, null));
    }

    @Test
    void recoveryCodes_generatesHashedUniqueCodes() {
        List<String> codes = totpService.generateRecoveryCodes(8);
        assertEquals(8, codes.size());

        for (String code : codes) {
            assertEquals(10, code.length());
            String hash = totpService.hashCode(code);
            assertNotNull(hash);
            assertEquals(64, hash.length(), "SHA-256 hash must be 64 hex characters");
        }
    }
}
