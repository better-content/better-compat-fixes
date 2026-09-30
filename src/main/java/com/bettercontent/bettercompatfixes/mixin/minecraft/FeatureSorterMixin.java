package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.compat.FeatureOrderSanitizer;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

@Mixin(FeatureSorter.class)
public abstract class FeatureSorterMixin {
    // This project does not emit a production refmap, so target the 1.20.1 SRG name explicitly.
    @WrapMethod(method = "m_220603_", remap = false)
    private static <T> List<FeatureSorter.StepFeatureData> betterContentFixes$removeRepeatedPlacedFeatures(
            final List<T> sources,
            final Function<T, List<HolderSet<PlacedFeature>>> features,
            final boolean indexFeatures,
            final Operation<List<FeatureSorter.StepFeatureData>> original
    ) {
        final Map<T, List<HolderSet<PlacedFeature>>> sanitized = new IdentityHashMap<>();
        int stepCount = 0;
        for (final T source : sources) {
            final List<HolderSet<PlacedFeature>> steps = FeatureOrderSanitizer.deduplicate(features.apply(source));
            sanitized.put(source, steps);
            stepCount = Math.max(stepCount, steps.size());
        }
        try {
            return original.call(sources, (Function<T, List<HolderSet<PlacedFeature>>>) sanitized::get, indexFeatures);
        } catch (final IllegalStateException cycle) {
            if (cycle.getMessage() == null || !cycle.getMessage().startsWith("Feature order cycle found")) throw cycle;
            final java.util.ArrayList<FeatureSorter.StepFeatureData> result = new java.util.ArrayList<>(stepCount);
            for (int step = 0; step < stepCount; step++) {
                final Object2IntOpenHashMap<PlacedFeature> indexes = new Object2IntOpenHashMap<>();
                indexes.defaultReturnValue(-1);
                final java.util.ArrayList<PlacedFeature> ordered = new java.util.ArrayList<>();
                for (final T source : sources) {
                    final List<HolderSet<PlacedFeature>> steps = sanitized.get(source);
                    if (step >= steps.size()) continue;
                    for (final net.minecraft.core.Holder<PlacedFeature> holder : steps.get(step)) {
                        final PlacedFeature feature = holder.value();
                        if (indexes.putIfAbsent(feature, indexes.size()) == -1) ordered.add(feature);
                    }
                }
                result.add(StepFeatureDataAccessor.betterContentFixes$create(ordered));
            }
            return List.copyOf(result);
        }
    }
}
