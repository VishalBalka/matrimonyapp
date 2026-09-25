package com.matrimonyapp.backend.security;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class TotpService {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final String HMAC_ALGO = "HmacSHA1";
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSecret() {
        byte[] buffer = new byte[20];
        secureRandom.nextBytes(buffer);
        return encodeBase32(buffer);
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || !code.matches("^[0-9]{6}$")) {
            return false;
        }

        long currentWindow = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;

        // Check window -1, 0, +1 for clock drift tolerance
        for (long i = -1; i <= 1; i++) {
            long window = currentWindow + i;
            String expected = generateTotpForWindow(secret, window);
            if (constantTimeEquals(expected, code)) {
                return true;
            }
        }
        return false;
    }

    public List<String> generateRecoveryCodes(int count) {
        List<String> codes = new ArrayList<>(count);
        char[] chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
        for (int i = 0; i < count; i++) {
            StringBuilder sb = new StringBuilder(10);
            for (int j = 0; j < 10; j++) {
                sb.append(chars[secureRandom.nextInt(chars.length)]);
            }
            codes.add(sb.toString());
        }
        return codes;
    }

    public String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.trim().toUpperCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String generateTotpForWindow(String base32Secret, long window) {
        try {
            byte[] key = decodeBase32(base32Secret);
            byte[] data = ByteBuffer.allocate(8).putLong(window).array();

            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(key, HMAC_ALGO));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            long truncatedHash = 0;
            for (int i = 0; i < 4; ++i) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xFF);
            }
            truncatedHash &= 0x7FFFFFFF;
            truncatedHash %= (long) Math.pow(10, CODE_DIGITS);

            return String.format("%0" + CODE_DIGITS + "d", truncatedHash);
        } catch (GeneralSecurityException e) {
            return "";
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int next = 0;
        int bitsLeft = 0;

        while (next < data.length) {
            buffer <<= 8;
            buffer |= (data[next++] & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                result.append(BASE32_CHARS.charAt(index));
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            result.append(BASE32_CHARS.charAt(index));
        }
        return result.toString();
    }

    private byte[] decodeBase32(String base32) {
        String clean = base32.trim().toUpperCase().replaceAll("[= \t\r\n]", "");
        ByteBuffer bytes = ByteBuffer.allocate(clean.length() * 5 / 8);
        int buffer = 0;
        int bitsLeft = 0;

        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) {
                continue;
            }
            buffer <<= 5;
            buffer |= val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                bytes.put((byte) ((buffer >> bitsLeft) & 0xFF));
            }
        }
        bytes.flip();
        byte[] result = new byte[bytes.remaining()];
        bytes.get(result);
        return result;
    }
}
