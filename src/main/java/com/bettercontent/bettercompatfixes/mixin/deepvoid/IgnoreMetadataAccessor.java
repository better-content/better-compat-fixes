package com.bettercontent.bettercompatfixes.mixin.deepvoid;

import com.bettercontent.bettercompatfixes.compat.deepvoid.FleshTemplateMetadata;
import com.google.common.collect.ImmutableList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockIgnoreProcessor.class, remap = false)
public abstract class IgnoreMetadataAccessor implements FleshTemplateMetadata.Ignore {
    @Shadow(aliases = {"f_74049_"}, remap = false) @Final
    private ImmutableList<Block> toIgnore;
    public ImmutableList<Block> better_compat_fixes$ignored() { return toIgnore; }
}
