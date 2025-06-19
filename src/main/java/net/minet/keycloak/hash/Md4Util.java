package net.minet.keycloak.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Utility for computing MD4 digests as hexadecimal strings. */
public final class Md4Util {
    private Md4Util() {
    }

    /**
     * Returns the MD4 hash of the given input encoded in UTF-16LE as a
     * lower-case hexadecimal string.
     */
    public static String md4Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD4");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_16LE));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD4 algorithm not available", e);
        }
    }
}
