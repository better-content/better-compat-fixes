package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.client.DeepVoidCompassTarget;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CompassItemPropertyFunction.class, remap = false)
public abstract class DeepVoidCompassMixin {
    @ModifyExpressionValue(method = {"getCompassRotation", "m_234954_"},
            at = {
                    @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/CompassItemPropertyFunction$CompassTarget;getPos(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/core/GlobalPos;", remap = false),
                    @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/CompassItemPropertyFunction$CompassTarget;m_234964_(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/core/GlobalPos;", remap = false)
            }, remap = false, require = 1, allow = 1)
    private GlobalPos better_compat_fixes$undergroundEntrance(GlobalPos vanilla,
            @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) ClientLevel level,
            @Local(argsOnly = true) Entity entity) {
        return DeepVoidCompassTarget.select(vanilla, level, stack, entity);
    }
}
