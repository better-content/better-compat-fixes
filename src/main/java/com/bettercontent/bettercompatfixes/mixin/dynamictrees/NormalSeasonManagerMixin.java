package com.bettercontent.bettercompatfixes.mixin.dynamictrees;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.ferreusveritas.dynamictrees.compat.season.NormalSeasonManager", remap = false)
public abstract class NormalSeasonManagerMixin {
    @Mutable
    @Final
    @Shadow(remap = false)
    private Map<ResourceLocation, Object> seasonContextMap;

    @Inject(method = "<init>*", at = @At("RETURN"), remap = false)
    private void better_compat_fixes$useConcurrentSeasonContextMap(final CallbackInfo ci) {
        if (!BcFixesConfig.dynamicTreesSeasonContextConcurrentMap()) {
            return;
        }
        this.seasonContextMap = new ConcurrentHashMap<>(this.seasonContextMap);
    }
}
