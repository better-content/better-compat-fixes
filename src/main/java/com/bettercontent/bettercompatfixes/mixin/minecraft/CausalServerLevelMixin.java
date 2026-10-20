package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.api.CausalMobSpawns;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/** Covers real fresh insertion results and both independent and passenger ticks. */
@Mixin(ServerLevel.class)
public abstract class CausalServerLevelMixin {
    @WrapMethod(method = {"tickNonPassenger", "m_8647_"}, remap = false, require = 1)
    private void betterContent$actorTick(Entity actor, Operation<Void> original) {
        CausalMobSpawns.runAs(actor, () -> original.call(actor));
    }

    @WrapMethod(method = {"tickPassenger", "m_8662_"}, remap = false, require = 1)
    private void betterContent$passengerTick(Entity vehicle, Entity actor, Operation<Void> original) {
        CausalMobSpawns.runAs(actor, () -> original.call(vehicle, actor));
    }

    @WrapMethod(method = {"addEntity", "m_8872_"}, remap = false, require = 1)
    private boolean betterContent$causalInsertion(Entity child, Operation<Boolean> original) {
        return CausalMobSpawns.insert(child, () -> original.call(child));
    }
}
