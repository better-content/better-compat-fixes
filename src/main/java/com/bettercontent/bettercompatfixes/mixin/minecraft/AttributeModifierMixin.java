package com.bettercontent.bettercompatfixes.mixin.minecraft;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AttributeModifier.class)
public abstract class AttributeModifierMixin {
    @Inject(
            method = {"load", "m_22212_"},
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void better_compat_fixes$ignoreMissingModifierTag(
            final CompoundTag tag,
            final CallbackInfoReturnable<AttributeModifier> cir
    ) {
        if (tag == null || !tag.hasUUID("UUID")) {
            cir.setReturnValue(null);
        }
    }
}
