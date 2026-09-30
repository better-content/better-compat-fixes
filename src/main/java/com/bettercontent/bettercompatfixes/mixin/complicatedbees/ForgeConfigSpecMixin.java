package com.bettercontent.bettercompatfixes.mixin.complicatedbees;

import com.bettercontent.bettercompatfixes.compat.ComplicatedBeesConfigCompat;
import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ForgeConfigSpec.class, remap = false)
public abstract class ForgeConfigSpecMixin {
    @Inject(method = "isCorrect", at = @At("HEAD"), remap = false)
    private void betterContentFixes$normalizeComplicatedBeesFloat(
            final CommentedConfig config,
            final CallbackInfoReturnable<Boolean> callback
    ) {
        ComplicatedBeesConfigCompat.normalizeResearchBonus(config);
    }
}
