package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.compat.ChoppingParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class ChoppingHitEffectsMixin {
    // Cover both normal hold and Forge-cancelled swings: no unrelated vanilla stream for wood.
    @Redirect(method = "continueAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleEngine;addBlockHitEffects(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;)V"), require = 2)
    private void bc$replaceWoodHits(ParticleEngine engine, BlockPos pos, BlockHitResult hit) {
        var level = ((Minecraft) (Object) this).level;
        if (level == null || !ChoppingParticles.isWood(level.getBlockState(pos))) engine.addBlockHitEffects(pos, hit);
    }
}
