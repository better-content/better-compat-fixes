package com.bettercontent.bettercompatfixes.mixin.parcool;

import com.alrex.parcool.common.action.impl.Dodge.DodgeDirection;
import com.bettercontent.bettercompatfixes.client.ArrowDoubleTapDodgeClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.alrex.parcool.common.action.impl.Dodge", remap = false)
public abstract class DodgeMixin {
    // ParCool 3.4.3.3 initializes its direction local to null before consulting native keys.
    // Supply an arrow request there, leaving all subsequent native eligibility checks intact.
    @ModifyVariable(method = "canStart", at = @At(value = "STORE", ordinal = 0), ordinal = 0, require = 1)
    private DodgeDirection betterContentFixes$arrowDirection(final DodgeDirection original) {
        final Vec3 move = ArrowDoubleTapDodgeClient.pendingMove();
        if (move == null) return original;
        if (move.x < 0) return DodgeDirection.Right;
        if (move.x > 0) return DodgeDirection.Left;
        return move.z > 0 ? DodgeDirection.Front : DodgeDirection.Back;
    }

    @ModifyExpressionValue(method = "canStart", at = @At(value = "INVOKE",
            target = "Lcom/alrex/parcool/client/input/KeyRecorder;getLastMoveVector()Lnet/minecraft/world/phys/Vec3;"),
            require = 1)
    private Vec3 betterContentFixes$arrowMoveVector(final Vec3 original) {
        final Vec3 move = ArrowDoubleTapDodgeClient.pendingMove();
        return move == null ? original : move;
    }

    @ModifyExpressionValue(
            method = "canStart",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/alrex/parcool/config/ParCoolConfig$Client$Booleans;get()Ljava/lang/Boolean;",
                    ordinal = 0),
            require = 1)
    private Boolean betterContentFixes$disableNativeDirectionalDoubleTap(final Boolean original) {
        return Boolean.FALSE;
    }
}
