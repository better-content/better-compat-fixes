package com.bettercontent.bettercompatfixes.mixin.epicfight;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.StaticAnimation;

/** Epic ParCool 20.12.0.1: leave fast running intact; only avoid stowing the held tool. */
@Pseudo
@Mixin(targets = "com.yesman.epicparcool.animations.ParCoolAnimations", remap = false)
public abstract class EpicParCoolFastRunStowMixin {
    // In the pinned biped/fast_run builder lambda this is the sole ON_BEGIN_EVENTS
    // entry, SET_TOOLS_BACK. Keep ON_END_EVENTS/REVERT_TO_HANDS and all other
    // animation builders unchanged. Guard the property as well as the ordinal.
    @WrapOperation(method = "lambda$build$17", at = @At(value = "INVOKE", ordinal = 0, remap = false,
            target = "Lyesman/epicfight/api/animation/types/StaticAnimation;addEvents(Lyesman/epicfight/api/animation/property/AnimationProperty$StaticAnimationProperty;[Lyesman/epicfight/api/animation/property/AnimationEvent;)Lyesman/epicfight/api/animation/types/StaticAnimation;"),
            require = 1, expect = 1, allow = 1, remap = false)
    private static StaticAnimation betterContentFixes$keepFastRunHeldItem(
            StaticAnimation animation, StaticAnimationProperty<?> property, AnimationEvent<?, ?>[] events,
            Operation<StaticAnimation> original) {
        return property == StaticAnimationProperty.ON_BEGIN_EVENTS
                ? animation : original.call(animation, property, events);
    }

    // The same builder's first addProperty installs SET_TOOLS_BACK_WHEN_ITEM_CHANGED.
    // Preserve head rotation, pose modifier, action states and the end event.
    @WrapOperation(method = "lambda$build$17", at = @At(value = "INVOKE", ordinal = 0, remap = false,
            target = "Lyesman/epicfight/api/animation/types/StaticAnimation;addProperty(Lyesman/epicfight/api/animation/property/AnimationProperty$StaticAnimationProperty;Ljava/lang/Object;)Lyesman/epicfight/api/animation/types/StaticAnimation;"),
            require = 1, expect = 1, allow = 1, remap = false)
    private static StaticAnimation betterContentFixes$keepChangedFastRunItem(
            StaticAnimation animation, StaticAnimationProperty<?> property, Object value,
            Operation<StaticAnimation> original) {
        return property == StaticAnimationProperty.ON_ITEM_CHANGE_EVENT
                ? animation : original.call(animation, property, value);
    }
}
