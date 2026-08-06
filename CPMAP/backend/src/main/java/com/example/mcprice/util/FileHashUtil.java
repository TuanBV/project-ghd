package com.example.mcprice.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class FileHashUtil {

    private FileHashUtil() {
    }

    public static String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 khong duoc ho tro", e);
        }
    }

    public static String rowIdentity(String... parts) {
        String joined = String.join("|", parts);
        return sha256(joined.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
