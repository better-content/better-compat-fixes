package com.bettercontent.bettercompatfixes.mixin.deepvoid;

import com.bettercontent.bettercompatfixes.compat.deepvoid.FleshTemplateMetadata;
import net.minecraft.server.level.WorldGenRegion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = WorldGenRegion.class, remap = false)
public abstract class RegionWriteRadiusAccessor implements FleshTemplateMetadata.Region {
    @Shadow(remap = false) @Final
    private int writeRadiusCutoff;
    public int better_compat_fixes$writeRadius() { return writeRadiusCutoff; }
}
