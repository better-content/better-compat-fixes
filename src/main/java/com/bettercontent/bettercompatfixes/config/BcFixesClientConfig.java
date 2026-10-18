package com.bettercontent.bettercompatfixes.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BcFixesClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue EPIC_FIGHT_HIDE_FIRST_PERSON_LIMBS;

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("epicFight");
        EPIC_FIGHT_HIDE_FIRST_PERSON_LIMBS = builder
                .comment(
                        "Hides the local player's body in Epic Fight first person.",
                        "Arms, sleeves and armor limbs stay hidden while swimming or submerged; held items remain visible.")
                .define("hideFirstPersonLimbs", true);
        builder.pop();

        SPEC = builder.build();
    }

    private BcFixesClientConfig() {
    }

    public static boolean hideFirstPersonLimbs() {
        return !SPEC.isLoaded() || EPIC_FIGHT_HIDE_FIRST_PERSON_LIMBS.get();
    }
}
