package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Membership is authored by Cultivars, not the food/foliage-contaminated native flower tags. */
public final class DecorativeFlowerPolicy {
    private static final TagKey<Block> DECORATIVE_FLOWERS = TagKey.create(Registries.BLOCK,
        new ResourceLocation("better_bumblezone_crops", "decorative_flowers"));

    private DecorativeFlowerPolicy() {}

    public static boolean isDecorative(BlockState state) {
        return state.is(DECORATIVE_FLOWERS);
    }
}
