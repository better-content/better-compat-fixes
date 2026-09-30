package com.bettercontent.bettercompatfixes.compat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicTreesBranchHardnessPolicyTest {
    @Test
    void inheritsEachSpeciesNativeLogHardnessAndPreservesUnbreakableLogs() {
        assertEquals(0.5F, DynamicTreesBranchHardnessPolicy.resolve(0.5F));
        assertEquals(1.5F, DynamicTreesBranchHardnessPolicy.resolve(1.5F));
        assertEquals(5.0F, DynamicTreesBranchHardnessPolicy.resolve(5.0F));
        assertEquals(-1.0F, DynamicTreesBranchHardnessPolicy.resolve(-1.0F));
    }

    @Test
    void missingOrNonFiniteNativeHardnessUsesPinnedDynamicTreesFallback() {
        assertEquals(2.0F, DynamicTreesBranchHardnessPolicy.resolve(null));
        assertEquals(2.0F, DynamicTreesBranchHardnessPolicy.resolve(Float.NaN));
        assertEquals(2.0F, DynamicTreesBranchHardnessPolicy.resolve(Float.POSITIVE_INFINITY));
    }

    @Test
    void pinnedDynamicTreesBranchMixinUsesFamilyPrimitiveLogAndIsRegistered() throws Exception {
        Path root = Path.of(".");
        String mixin = Files.readString(root.resolve(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/dynamictrees/BranchHardnessMixin.java"));
        String config = Files.readString(root.resolve("src/main/resources/better_compat_fixes.mixins.json"));
        String plugin = Files.readString(root.resolve(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/BetterContentMixinPlugin.java"));
        String commonConfig = Files.readString(root.resolve(
                "src/main/java/com/bettercontent/bettercompatfixes/config/BcFixesConfig.java"));
        String pinnedMod = Files.readString(root.resolve("../../better-content-modpack/mods/dynamictrees.pw.toml"));

        assertTrue(mixin.contains("!BcFixesConfig.dynamicTreesSpeciesHardness()"));
        assertTrue(mixin.contains("getFamily(state, level, pos)"));
        assertTrue(mixin.contains("getPrimitiveLog()"));
        assertTrue(mixin.contains("getDestroySpeed(level, pos)"));
        assertTrue(config.contains("\"dynamictrees.BranchHardnessMixin\""));
        assertTrue(plugin.contains("if (DYNAMIC_TREES_BRANCH_HARDNESS_MIXIN.equals(mixinClassName))"));
        assertTrue(plugin.contains("return hasVersion(mods, \"dynamictrees\", \"1.20.1-1.4.10\")"));
        assertTrue(commonConfig.contains(".define(\"speciesHardness\", true)"));
        assertTrue(pinnedMod.contains("filename = \"DynamicTrees-1.20.1-1.4.10.jar\""));
        assertTrue(pinnedMod.contains("hash = \"6fdd23dfdbdf0fa88c468b1cae6a2dbefa019923\""));
    }
}
