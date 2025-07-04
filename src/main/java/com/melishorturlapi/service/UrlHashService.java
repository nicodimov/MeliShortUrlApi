package com.melishorturlapi.service;

import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class UrlHashService {
    private int codeLength = 6;
    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public UrlHashService() {}
    public UrlHashService(int codeLength) {
        this.codeLength = codeLength;
    }
    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }
    public int getCodeLength() {
        return codeLength;
    }

    public String hashUrl(String longUrl) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(longUrl.getBytes(StandardCharsets.UTF_8));
            return encodeBase62(hash).substring(0, codeLength);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String encodeBase62(byte[] input) {
        StringBuilder sb = new StringBuilder();
        int value = 0;
        int bitCount = 0;
        for (byte b : input) {
            value = (value << 8) | (b & 0xFF);
            bitCount += 8;
            while (bitCount >= 6) {
                bitCount -= 6;
                int idx = (value >> bitCount) & 0x3F;
                sb.append(BASE62.charAt(idx % BASE62.length()));
            }
        }
        if (sb.length() < codeLength) {
            while (sb.length() < codeLength) {
                sb.append('0');
            }
        }
        return sb.toString();
    }
} 