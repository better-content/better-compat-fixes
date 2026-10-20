package com.bettercontent.bettercompatfixes.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BcFixesClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue EPIC_FIGHT_HIDE_FIRST_PERSON_LIMBS;
    public static final ForgeConfigSpec.IntValue BEE_SWARM_DENSITY;

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("epicFight");
        EPIC_FIGHT_HIDE_FIRST_PERSON_LIMBS = builder
                .comment(
                        "Hides the local player's body in Epic Fight first person.",
                        "Arms, sleeves and armor limbs stay hidden while swimming or submerged; held items remain visible.")
                .define("hideFirstPersonLimbs", true);
        builder.pop();

        builder.push("complicatedBees");
        BEE_SWARM_DENSITY = builder.comment("Cosmetic colony swarms: 0 disables; 1 reduced; 2 normal; 3-4 denser. Minimal particles always disables swarms.")
                .defineInRange("swarmDensity", 2, 0, 4);
        builder.pop();
        SPEC = builder.build();
    }

    private BcFixesClientConfig() {
    }

    public static int beeSwarmDensity() {
        return SPEC.isLoaded() ? BEE_SWARM_DENSITY.get() : 2;
    }

    public static boolean hideFirstPersonLimbs() {
        return !SPEC.isLoaded() || EPIC_FIGHT_HIDE_FIRST_PERSON_LIMBS.get();
    }
}
