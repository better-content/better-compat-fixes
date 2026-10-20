package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.api.CausalMobSpawns;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Slime.class)
public abstract class CausalSlimeMixin {
    @WrapMethod(method = {"remove", "m_142687_"}, remap = false, require = 1)
    private void betterContent$splitCause(Entity.RemovalReason reason, Operation<Void> original) {
        CausalMobSpawns.runAs((Entity) (Object) this, () -> original.call(reason));
    }
}
