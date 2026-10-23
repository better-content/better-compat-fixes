package com.bettercontent.bettercompatfixes.compat;

/** Pure admission contract shared by the runtime optional plugin and negative controls. */
public final class TreasureUrnCompatibility {
    private TreasureUrnCompatibility() { }
    public static boolean checkVersion(boolean present, String version) {
        if (!present) return false;
        if (!"1.20-3.1.43".equals(version)) throw new IllegalStateException("Treasure urn compatibility requires Supplementaries 1.20-3.1.43");
        return true;
    }
    public static void checkClasses(String urn, String tile) {
        boolean published = "b088bcc23b678abdfb479c3c33ab9e0d4d3fae2f46c81eb111cd4f4ee10653c7".equals(urn)
                && "a3cabed968d4e0c09e34b8e933845291595f8e91a3f0de5e0031730288a00cf5".equals(tile);
        boolean mapped = "68884aeaa5e9389ca631a2f185e799b2fc215c3f3b239905934437fc376fceff".equals(urn)
                && "8112ff27f15ee53f2c164acdac18c035eb15f3a0bee99d763a46e441c565d435".equals(tile);
        if (!published && !mapped) throw new IllegalStateException("Unreviewed Supplementaries urn class pair: " + urn + "/" + tile);
    }
}
