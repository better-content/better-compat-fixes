package com.bettercontent.bettercompatfixes.mixin.minecraft;

import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(FeatureSorter.StepFeatureData.class)
public interface StepFeatureDataAccessor {
    @Invoker("<init>")
    static FeatureSorter.StepFeatureData betterContentFixes$create(final List<PlacedFeature> features) {
        throw new AssertionError();
    }
}
