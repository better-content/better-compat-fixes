package com.bettercontent.bettercompatfixes.mixin.iceandfire;

import com.bettercontent.bettercompatfixes.api.CausalMobSpawns;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Ice and Fire dispatches this custom kill callback outside the killer's normal tick. */
@Mixin(targets = "com.github.alexthe666.iceandfire.entity.EntityDreadMob", remap = false)
public abstract class DreadMobCausalMixin {
    @WrapMethod(method = "onKillEntity", remap = false, require = 1)
    private void betterContent$necromancyCause(LivingEntity victim, Operation<Void> original) {
        CausalMobSpawns.runAs((Entity) (Object) this, () -> original.call(victim));
    }
}
