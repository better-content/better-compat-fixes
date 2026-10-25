package com.bettercontent.bettercompatfixes.mixin.epicfight;

import com.bettercontent.bettercompatfixes.compat.OptionalTinkerSkinPolicy;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

// All MAIN merges precede injection preparation. Priority > upstream's 1000 permits
// injecting into its non-Unique helper after it has been merged into the real listener.
@Mixin(targets = "yesman.epicfight.api.client.model.ItemSkinsReloadListener", priority = 1100, remap = false)
public abstract class OptionalTinkerItemSkinsMixin {
    @Inject(method = "addTinkerWeapon(Ljava/util/Map;Ljava/lang/String;DDDDDDI)V",
            at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void bettercompatfixes$registeredOptionalSkin(Map<?, ?> skins, String item,
            double x1, double y1, double z1, double x2, double y2, double z2, int lifetime, CallbackInfo ci) {
        // Guard synthesis, not Epic Fight's validation/logging or authored skin map.
        if (OptionalTinkerSkinPolicy.skipSyntheticSkin(item,
                id -> ForgeRegistries.ITEMS.containsKey(new ResourceLocation(id)))) ci.cancel();
    }
}
