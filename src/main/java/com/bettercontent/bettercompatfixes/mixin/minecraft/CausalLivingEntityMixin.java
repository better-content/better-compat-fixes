package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.api.CausalMobSpawns;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** A victim's synchronous death/damage products belong to the victim, never its attacker. */
@Mixin(LivingEntity.class)
public abstract class CausalLivingEntityMixin {
    @WrapMethod(method = {"hurt", "m_6469_"}, remap = false, require = 1)
    private boolean betterContent$damageCause(DamageSource source, float amount, Operation<Boolean> original) {
        return CausalMobSpawns.as((Entity) (Object) this, () -> original.call(source, amount));
    }

    @WrapMethod(method = {"die", "m_6667_"}, remap = false, require = 1)
    private void betterContent$deathCause(DamageSource source, Operation<Void> original) {
        CausalMobSpawns.runAs((Entity) (Object) this, () -> original.call(source));
    }
}
