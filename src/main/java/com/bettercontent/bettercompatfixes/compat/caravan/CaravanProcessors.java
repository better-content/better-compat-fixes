package com.bettercontent.bettercompatfixes.compat.caravan;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

/** Optional BIC registration; all types are vanilla/owned, with no native BIC class linkage. */
public final class CaravanProcessors {
    private static final DeferredRegister<StructureProcessorType<?>> TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, BetterContentFixes.MOD_ID);
    static { TYPES.register("caravan_frame_anchor", () -> CaravanAnchorProcessor.TYPE); }
    private CaravanProcessors() { }
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
