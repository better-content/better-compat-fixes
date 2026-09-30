package com.bettercontent.bettercompatfixes.mixin.epicfight;

import com.bettercontent.bettercompatfixes.client.ZeroSkillHudPolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;

@Mixin(value = BattleModeGui.class, remap = false)
public abstract class BattleModeGuiMixin {
    @Redirect(method = "renderNormalSkills",
            at = @At(value = "INVOKE",
                    target = "Lyesman/epicfight/skill/Skill;shouldDraw(Lyesman/epicfight/skill/SkillContainer;)Z"),
            require = 1)
    private boolean betterContentFixes$hideZeroNormalSkill(
            final Skill skill, final SkillContainer container) {
        return skill.shouldDraw(container) && ZeroSkillHudPolicy.hasVisibleValue(skill, container);
    }

    @Redirect(method = "lambda$renderNormalSkills$0",
            at = @At(value = "INVOKE",
                    target = "Lyesman/epicfight/skill/Skill;shouldDraw(Lyesman/epicfight/skill/SkillContainer;)Z"),
            require = 1)
    private static boolean betterContentFixes$hideZeroSkillInLayout(
            final Skill skill, final SkillContainer container) {
        return skill.shouldDraw(container) && ZeroSkillHudPolicy.hasVisibleValue(skill, container);
    }

    @Redirect(method = "renderWeaponInnateSkill",
            at = @At(value = "INVOKE",
                    target = "Lyesman/epicfight/skill/Skill;shouldDraw(Lyesman/epicfight/skill/SkillContainer;)Z"),
            require = 1)
    private boolean betterContentFixes$hideZeroWeaponSkill(
            final Skill skill, final SkillContainer container) {
        return skill.shouldDraw(container) && ZeroSkillHudPolicy.hasVisibleValue(skill, container);
    }
}
