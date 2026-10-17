package com.bettercontent.bettercompatfixes.mixin.epicfight;

import com.bettercontent.bettercompatfixes.client.FirstPersonPovPriority;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.Function;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

@Mixin(value = LocalPlayerPatch.class, remap = false)
public abstract class LocalPlayerAttackPovMixin {
    @WrapOperation(
            method = "clientTick(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingTickEvent;)V",
            at = @At(value = "INVOKE", target =
                    "Lyesman/epicfight/api/client/animation/ClientAnimator;iterVisibleLayersUntilFalse(Ljava/util/function/Function;)Z"),
            require = 1,
            expect = 1,
            allow = 1)
    private boolean betterContentFixes$preferSwimmingAttackPose(
            final ClientAnimator animator,
            final Function<Layer, Boolean> nativeSelector,
            final Operation<Boolean> original
    ) {
        final LocalPlayerPatch player = (LocalPlayerPatch) (Object) this;
        final LivingMotion motion = animator.currentMotion();
        return original.call(animator, FirstPersonPovPriority.guard(
                motion == LivingMotions.SWIM || motion == LivingMotions.FLOAT,
                nativeSelector,
                layer -> !layer.isOff()
                        && layer.animationPlayer.getRealAnimation().get() instanceof AttackAnimation attack
                        && attack.getProperty(ClientAnimationProperties.POV_ANIMATION).isEmpty(),
                // Native off() empties this layer. The rest of clientTick then clears povSettings,
                // so FirstPersonRenderer uses the real combined pose, including the attack overlay.
                () -> player.getFirstPersonLayer().off()
        ));
    }
}
