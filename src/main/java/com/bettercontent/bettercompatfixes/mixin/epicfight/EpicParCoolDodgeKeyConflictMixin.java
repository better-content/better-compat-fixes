package com.bettercontent.bettercompatfixes.mixin.epicfight;

import com.bettercontent.bettercompatfixes.client.ArrowDoubleTapDodgeClient;
import com.bettercontent.bettercompatfixes.client.DodgeKeyConflictPolicy;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import yesman.epicfight.skill.Skill;

/** Keep Epic ParCool's stamina check, but do not treat arrows as a direct-key collision. */
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
}
