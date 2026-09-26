package com.bettercontent.bettercontentfixes.mixin.untamedwilds;

import com.bettercontent.bettercontentfixes.compat.UntamedClientEntityData;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import untamedwilds.util.EntityDataHolder;

/** The mod loads species data on servers but may construct a client whale before that data exists. */
@Mixin(targets = "untamedwilds.entity.ComplexMob", remap = false)
public abstract class ComplexMobMixin {
    @Inject(method = "getEntityData", at = @At("RETURN"), cancellable = true, remap = false, require = 1)
    private static void better_content_fixes$loadClientEntityData(
            final EntityType<?> type,
            final CallbackInfoReturnable<EntityDataHolder> result
    ) {
        if (result.getReturnValue() == null) {
            final EntityDataHolder fallback = UntamedClientEntityData.load(type);
            if (fallback != null) result.setReturnValue(fallback);
        }
    }
}
