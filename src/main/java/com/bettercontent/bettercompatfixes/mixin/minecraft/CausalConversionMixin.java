package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.api.CausalMobSpawns;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Mob.class)
public abstract class CausalConversionMixin {
    @WrapMethod(method = {"convertTo", "m_21406_"}, remap = false, require = 1)
    private Mob betterContent$conversionCause(EntityType<? extends Mob> type, boolean equipment,
                                              Operation<Mob> original) {
        return CausalMobSpawns.as((Mob) (Object) this, () -> original.call(type, equipment));
    }
}
