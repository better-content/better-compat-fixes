package com.bettercontent.bettercompatfixes.mixin.epicfightvs;

import com.bettercontent.bettercompatfixes.compat.epicfightvs.EpicFightVsAttackTransforms;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.MultiCollider;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = MultiCollider.class, remap = false)
public abstract class MultiColliderMixin {
    @ModifyArg(
            method = "updateAndSelectCollideEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lyesman/epicfight/api/collider/Collider;transform(Lyesman/epicfight/api/utils/math/OpenMatrix4f;)V"
            ),
            index = 0,
            require = 1
    )
    private OpenMatrix4f betterContentFixes$transformMountedAttack(
            final OpenMatrix4f transform,
            @Local(argsOnly = true) final LivingEntityPatch<?> entityPatch
    ) {
        return EpicFightVsAttackTransforms.shipToWorld(transform, entityPatch);
    }
}
