package com.bettercontent.bettercompatfixes.mixin.tconstruct;

import com.bettercontent.bettercompatfixes.api.CausalMobSpawns;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/** ArmoredSlimeEntity implements its own removal/split loop instead of Slime.remove. */
@Mixin(targets = "slimeknights.tconstruct.world.entity.ArmoredSlimeEntity", remap = false)
public abstract class ArmoredSlimeCausalMixin {
    @WrapMethod(method = {"remove", "m_142687_"}, remap = false, require = 1)
    private void betterContent$splitCause(Entity.RemovalReason reason, Operation<Void> original) {
        CausalMobSpawns.runAs((Entity) (Object) this, () -> original.call(reason));
    }
}
