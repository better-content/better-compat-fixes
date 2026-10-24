package com.bettercontent.bettercompatfixes.compat.deepvoid;

import java.util.Set;
import static com.bettercontent.bettercompatfixes.compat.deepvoid.FleshTemplateBounds.require;

/** Primitive property admission shared by the actual metadata scan and non-native controls. */
public final class FleshTemplateSemantics {
    private static final Set<String> IDS = Set.of(
            "the_deep_void:flesh_rib1", "the_deep_void:flesh_rib2", "the_deep_void:flesh_ribs1",
            "the_deep_void:giant_rib1", "the_deep_void:giant_rib2", "the_deep_void:fleshy_giant_skull",
            "the_deep_void:flesh_ribs_eyes", "the_deep_void:flesh_tentacles_new");
    private static final Set<String> BONE_IDS = Set.of(
            "the_deep_void:broken_bone1", "the_deep_void:broken_bone2", "the_deep_void:broken_bone3new");
    private FleshTemplateSemantics() {}
    public static boolean recognizes(String id) { return IDS.contains(id) || isBrokenBone(id); }
    public static boolean isBrokenBone(String id) { return BONE_IDS.contains(id); }
    public static void boneSettings(int processors, boolean exactProcessor, int ignored,
                                    boolean air, boolean structure, boolean zeroPivot, boolean nullBox, int flags) {
        require(processors == 1 && exactProcessor && ignored == 1 && !air && structure, "bone-ignore-processor");
        require(zeroPivot && nullBox && flags == 4, "placement-settings");
    }
    public static void settings(int processors, boolean exactProcessor, int ignored,
                                boolean air, boolean structure, boolean zeroPivot, boolean nullBox, int flags) {
        require(processors == 1 && exactProcessor && ignored == 2 && air && structure, "ignore-processor");
        require(zeroPivot && nullBox && flags == 4, "placement-settings");
    }
    public static void size(int x, int y, int z, int palettes, boolean noEntities) {
        require(x > 0 && y > 0 && z > 0 && x <= 46 && z <= 46, "size");
        require(palettes > 0 && noEntities, "palettes-or-entities");
    }
    public static void block(int x, int y, int z, int sx, int sy, int sz,
                             boolean knownClassAndId, boolean waterlogged, boolean payload, boolean ignoredStructure) {
        require(x >= 0 && x < sx && y >= 0 && y < sy && z >= 0 && z < sz, "local-position");
        require(knownClassAndId && !waterlogged, "block-state");
        require(!payload || ignoredStructure, "block-nbt");
    }
}
