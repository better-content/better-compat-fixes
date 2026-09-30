package com.bettercontent.bettercompatfixes.client;

import net.minecraft.world.entity.player.Player;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.skill.SkillDataKeys;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.skill.identity.RevelationSkill;
import yesman.epicfight.skill.passive.AdaptiveSkinSkill;
import yesman.epicfight.skill.passive.BerserkerSkill;
import yesman.epicfight.skill.passive.BonebreakerSkill;
import yesman.epicfight.skill.passive.HyperVitalitySkill;
import yesman.epicfight.skill.passive.PassiveSkill;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;

/** Hides skill widgets whose displayed numeric state would be zero. */
public final class ZeroSkillHudPolicy {
    private ZeroSkillHudPolicy() {
    }

    public static boolean hasVisibleValue(final Skill skill, final SkillContainer container) {
        if (skill instanceof GuardSkill) {
            return roundedPositive(number(container, SkillDataKeys.PENALTY.get()) * 10.0F);
        }
        if (skill instanceof RevelationSkill || skill instanceof AdaptiveSkinSkill
                || skill instanceof BonebreakerSkill) {
            return number(container, SkillDataKeys.STACKS.get()) > 0.0F;
        }
        if (skill instanceof BerserkerSkill) {
            Player player = (Player) container.getExecutor().getOriginal();
            return player.getMaxHealth() > 0.0F
                    && roundedPositive((player.getMaxHealth() - player.getHealth())
                            * 100.0F / player.getMaxHealth());
        }
        if (skill instanceof HyperVitalitySkill) {
            return roundedPositive(container.getResource());
        }
        if (skill instanceof WeaponInnateSkill) {
            if (container.isActivated()) return roundedPositive(container.getRemainDuration() / 20.0F);
            return container.getStack() > 0 || container.getResource() > 0.0F;
        }
        if (skill instanceof PassiveSkill) {
            return roundedPositive(container.getMaxResource() - container.getResource());
        }
        return true;
    }

    static boolean roundedPositive(final float displayed) {
        return Float.isFinite(displayed) && Math.round(displayed) > 0;
    }

    private static float number(final SkillContainer container, final SkillDataKey<?> key) {
        Object value = container.getDataManager().getDataValue(key);
        return value instanceof Number number ? number.floatValue() : 0.0F;
    }
}
