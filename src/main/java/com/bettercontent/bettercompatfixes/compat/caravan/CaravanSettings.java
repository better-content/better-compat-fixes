package com.bettercontent.bettercompatfixes.compat.caravan;

import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Pure attachment policy. Optional-mod admission is performed before the owning Mixin loads. */
public final class CaravanSettings {
    public static final ResourceLocation TEMPLATE = ResourceLocation.tryParse("born_in_chaos_v1:clown_caravan");
    private CaravanSettings() { }
    public static StructurePlaceSettings attach(Either<ResourceLocation, StructureTemplate> template,
                                                StructurePlaceSettings original) {
        if (!template.left().filter(TEMPLATE::equals).isPresent()
                || original.getProcessors().contains(CaravanAnchorProcessor.INSTANCE)) return original;
        return original.copy().addProcessor(CaravanAnchorProcessor.INSTANCE);
    }
}
