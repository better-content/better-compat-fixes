package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Shared natural and modded terrain that death worms can navigate and burrow through. */
public final class DeathWormTerrain {
    public static final TagKey<Block> BURROWABLE = TagKey.create(Registries.BLOCK,
        new ResourceLocation("better_compat_fixes", "death_worm_burrowable"));
    private DeathWormTerrain() {}
}
