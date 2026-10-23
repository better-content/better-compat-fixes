package com.bettercontent.bettercompatfixes.compat.registrate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/** Whole original class pairs, independently pinned raw and official-mapped; never initializes targets. */
public final class RegistrateCompatibility {
    private RegistrateCompatibility() {}
    private static final Set<List<String>> PAIRS = Set.of(
        List.of("7bfa4965a2cf3fc790227e6d94bb47d5b9a898b532f4f489570db2ae158c4147", "37bb5b0230e0a7d9fb594c9ec53d37317094db089035b4e0f19282f06ba780ee"),
        List.of("a2052e843b729659493d725127556df6931ddf5a3aab2937ef1e48cf1318034f", "e1e9154a7036be8584b81bede8f7c41cb1a0301e3c16c664e71e7d0d2346d029"));
    public static boolean check(byte[] owner, byte[] registration) {
        if (owner == null && registration == null) return false;
        if (owner == null || registration == null || !PAIRS.contains(List.of(hash(owner), hash(registration))))
            throw new IllegalStateException("Unsupported present Registrate construction ABI pair");
        return true;
    }
    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
