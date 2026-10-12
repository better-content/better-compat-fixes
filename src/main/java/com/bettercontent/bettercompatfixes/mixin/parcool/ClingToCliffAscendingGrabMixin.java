package com.bettercontent.bettercompatfixes.mixin.parcool;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Allows a reachable ledge to be grabbed during the upward part of a jump. */
@Mixin(targets = "com.alrex.parcool.common.action.impl.ClingToCliff", remap = false)
public abstract class ClingToCliffAscendingGrabMixin {
    @ModifyConstant(
            method = "canStart(Lnet/minecraft/world/entity/player/Player;Lcom/alrex/parcool/common/capability/Parkourability;Lcom/alrex/parcool/common/capability/IStamina;Ljava/nio/ByteBuffer;)Z",
            constant = @Constant(doubleValue = 0.2D),
            require = 1,
            expect = 1,
            allow = 1,
            remap = false)
    private double betterContentFixes$allowAscendingLedgeGrab(final double original) {
        // Only remove the upward-speed ceiling. Native input, stamina, wall-running,
        // facing, collision/reach and surface-friction checks remain unchanged.
        return Double.POSITIVE_INFINITY;
    }
}
