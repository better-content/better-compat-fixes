package com.bettercontent.bettercompatfixes.mixin.deepvoid;

import com.bettercontent.bettercompatfixes.compat.deepvoid.FleshTemplateMetadata;
import net.minecraft.server.level.WorldGenRegion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Exact production field name: Mixin aliases are forbidden after access widening. */
@Mixin(value = WorldGenRegion.class, remap = false)
public abstract class RegionWriteRadiusSrgAccessor implements FleshTemplateMetadata.Region {
    @Shadow(remap = false) @Final
    private int f_143481_;
    public int better_compat_fixes$writeRadius() { return f_143481_; }
}
