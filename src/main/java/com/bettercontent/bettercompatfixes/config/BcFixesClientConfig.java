package com.bettercontent.bettercompatfixes.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BcFixesClientConfig {
    public static final int CURRENT_KEYMAP_PROFILE_VERSION = 2;
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue EPIC_FIGHT_HIDE_FIRST_PERSON_LIMBS;
    public static final ForgeConfigSpec.BooleanValue APPLY_RECOMMENDED_KEYMAP_MIGRATION;
    public static final ForgeConfigSpec.IntValue KEYMAP_PROFILE_VERSION;

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("epicFight");
        EPIC_FIGHT_HIDE_FIRST_PERSON_LIMBS = builder
                .comment(
                        "Hides the local player's body in Epic Fight first person.",
                        "Animated arms remain visible while swimming or submerged.")
                .define("hideFirstPersonLimbs", true);
        builder.pop();

        builder.push("controls");
        APPLY_RECOMMENDED_KEYMAP_MIGRATION = builder
                .comment("Migrates unchanged legacy pack bindings to the current recommended control profile once.")
                .define("applyRecommendedKeymapMigration", true);
        KEYMAP_PROFILE_VERSION = builder
                .comment("Internal version marker for completed recommended-keymap migrations.")
                .defineInRange("keymapProfileVersion", 0, 0, CURRENT_KEYMAP_PROFILE_VERSION);
        builder.pop();

        SPEC = builder.build();
    }

    private BcFixesClientConfig() {
    }

    public static boolean hideFirstPersonLimbs() {
        return !SPEC.isLoaded() || EPIC_FIGHT_HIDE_FIRST_PERSON_LIMBS.get();
    }

    public static boolean shouldMigrateRecommendedKeymap() {
        return SPEC.isLoaded()
                && APPLY_RECOMMENDED_KEYMAP_MIGRATION.get()
                && KEYMAP_PROFILE_VERSION.get() < CURRENT_KEYMAP_PROFILE_VERSION;
    }
}
