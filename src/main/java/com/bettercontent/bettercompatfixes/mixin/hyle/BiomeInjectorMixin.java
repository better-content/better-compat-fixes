package com.bettercontent.bettercompatfixes.mixin.hyle;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "lilypuree.hyle.BiomeInjector", remap = false)
public abstract class BiomeInjectorMixin {
    @Redirect(
            method = "apply(Lnet/minecraft/core/RegistryAccess;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/levelgen/GenerationStep$Decoration;LOCAL_MODIFICATIONS:Lnet/minecraft/world/level/levelgen/GenerationStep$Decoration;"),
            remap = false)
    private static GenerationStep.Decoration better_compat_fixes$runAfterUndergroundDecoration() {
        if (BcFixesConfig.hyleRunAfterUndergroundDecoration()) {
            return GenerationStep.Decoration.UNDERGROUND_DECORATION;
        }
        return GenerationStep.Decoration.LOCAL_MODIFICATIONS;
    }
}
