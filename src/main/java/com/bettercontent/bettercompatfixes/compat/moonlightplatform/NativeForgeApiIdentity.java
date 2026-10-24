package com.bettercontent.bettercompatfixes.compat.moonlightplatform;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.ZipInputStream;

/** Closed identity contract, not a generic Forge/Fabric support detector. */
public final class NativeForgeApiIdentity {
    public static final String MOONLIGHT_CLASS = "net/mehvahdjukaar/moonlight/forge/MoonlightForge.class";
    public enum Role { PRODUCTION_SRG, NATIVE_SDK_OFFICIAL }
    public record Pair(String root, String moonlight, String moonlightClass, String forge, Map<String, String> modules) {}
    public static final Map<String, String> MODULE_PATHS = Map.of(
            "fabric_api_base", "META-INF/jarjar/fabric-api-base-0.4.32+ef105b4977.jar",
            "fabric_events_interaction_v0", "META-INF/jarjar/fabric-events-interaction-v0-0.6.5+0f26153f77.jar",
            "fabric_resource_loader_v0", "META-INF/jarjar/fabric-resource-loader-v0-0.11.12+4b4151c077.jar");
    // Whole root hashes bind EVERY nested archive, class and resource, not only three API modules.
    public static final Pair RAW = new Pair(
            "429b25aaa42c88393cafca04c0e2b9ecfcfc277e14fc3fb86045877144541742",
            "007c05ae799417ca7aa26669c5d97be9dface5b6fc27aca4ca8031cafe4ddd6e",
            "494ca46af0e921a738da1aa198cf674333063370c0cafc834241780dc19b24b9", "47.4.22", Map.of(
            "fabric_api_base", "2e2e97e642a9578d2f1f8234ac4d567fa6e6c50318602e29d4accb928beb4394",
            "fabric_events_interaction_v0", "5beb96117fb20294208c413fa697afa4745fa2784b893ba3be1fb35942640647",
            "fabric_resource_loader_v0", "905f3bdfd3dfc1b32ffa11f26c63e9a800ab18878a0aea4fde7330f74bc91862"));
    // Explicit nonproduction SDK role: Forge installertools 1.4.4 SRG_TO_MCP, official 1.20.1.
    // Raw archive bytes are NOT compared with transformed classloader resources.
    public static final Pair SDK = new Pair(
            "447b9e285717e66e95924e190a13f5a2079b969af70ed4c6577d40cad9423351",
            "ec130f96cb65ce97d600880127322656021bd5c6e60a976de9eb9403e5f3d5a3",
            "cffa40fb51a28a80ac19a8541ea5b2b4a31d2f13f1bf6a4090b4a7bbeeedb92f", "47.4.13", Map.of(
            "fabric_api_base", "ccfa049091ce36c8ba4fab642c5c7291f4eb7729afed65a43ec1bebd999550a9",
            "fabric_events_interaction_v0", "bc2e6f06e8791cc98fce43408427d6a5125645c75abb153abdcfa009c3ba458c",
            "fabric_resource_loader_v0", "254113f0d62b8b953abe3a20f053060a61ad21c480304442b80275a2389e82de"));
    private NativeForgeApiIdentity() {}
    public static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public static byte[] read(Path path) throws IOException {
        // Forge-owned paths may be nested/virtual; do not infer identity from path spelling.
        try (var input = Files.newInputStream(path)) {
            byte[] bytes = input.readNBytes(64 * 1024 * 1024 + 1);
            if (bytes.length > 64 * 1024 * 1024) throw new IOException("Provider archive exceeds identity budget");
            return bytes;
        }
    }
    public static Map<String, byte[]> entries(byte[] archive) throws IOException {
        Map<String, byte[]> result = new HashMap<>();
        long expanded = 0;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                byte[] body = zip.readNBytes(64 * 1024 * 1024 + 1);
                expanded += body.length;
                if (body.length > 64 * 1024 * 1024 || expanded > 192 * 1024 * 1024 || result.size() > 20000
                        || result.putIfAbsent(entry.getName(), body) != null) throw new IOException("Ambiguous/budgeted provider archive");
            }
        }
        return result;
    }
    public static Pair pair(boolean production, byte[] root, byte[] moonlight) {
        if (root == null || moonlight == null) return null;
        String r = hash(root), m = hash(moonlight);
        if (r.equals(RAW.root()) && m.equals(RAW.moonlight())) return RAW;
        if (!production && r.equals(SDK.root()) && m.equals(SDK.moonlight())) return SDK;
        return null;
    }
}
