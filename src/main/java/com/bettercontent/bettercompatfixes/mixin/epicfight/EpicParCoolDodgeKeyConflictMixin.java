package com.bettercontent.bettercompatfixes.mixin.epicfight;

import com.bettercontent.bettercompatfixes.client.ArrowDodgeAnimationInputs;
import com.bettercontent.bettercompatfixes.client.ArrowDoubleTapDodgeClient;
import com.bettercontent.bettercompatfixes.client.DodgeKeyConflictPolicy;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import yesman.epicfight.skill.Skill;

/** Bridge independent arrow input into native Epic ParCool admission and animation selection. */
@Pseudo
@Mixin(targets = "com.yesman.epicparcool.event.ParCoolEvents", remap = false)
public abstract class EpicParCoolDodgeKeyConflictMixin {
    @ModifyExpressionValue(
            method = "lambda$onSetup$17",
            at = @At(value = "INVOKE", target =
                    "Lyesman/epicfight/skill/SkillContainer;getSkill()Lyesman/epicfight/skill/Skill;"),
            require = 1, expect = 1, allow = 1)
    private static Skill betterContentFixes$separateArrowRequest(final Skill original) {
        // This expression is used only by the key-collision guard. It does not unequip a
        // skill, cancel the whole event handler, or skip the subsequent stamina check.
        return DodgeKeyConflictPolicy.skillForDirectKeyConflict(
                original, ArrowDoubleTapDodgeClient.pendingMove() != null);
    }

    // Native animation selection reads vanilla Input, which arrows intentionally do not
    // mutate. Cover production SRG and mapped development names without changing key state.
    @ModifyExpressionValue(method = "lambda$onSetup$8", at = {
            @At(value = "FIELD", target = "Lnet/minecraft/client/player/Input;f_108566_:F"),
            @At(value = "FIELD", target = "Lnet/minecraft/client/player/Input;leftImpulse:F")
    }, require = 2, expect = 2, allow = 2)
    private static float betterContentFixes$arrowAnimationLeft(final float original) {
        final Vec3 move = ArrowDoubleTapDodgeClient.pendingMove();
        return ArrowDodgeAnimationInputs.impulse(original, move == null ? 0 : move.x, move != null);
    }

    @ModifyExpressionValue(method = "lambda$onSetup$8", at = {
            @At(value = "FIELD", target = "Lnet/minecraft/client/player/Input;f_108567_:F"),
            @At(value = "FIELD", target = "Lnet/minecraft/client/player/Input;forwardImpulse:F")
    }, require = 1, expect = 1, allow = 1)
    private static float betterContentFixes$arrowAnimationForward(final float original) {
        final Vec3 move = ArrowDoubleTapDodgeClient.pendingMove();
        return ArrowDodgeAnimationInputs.impulse(original, move == null ? 0 : move.z, move != null);
    }
}
