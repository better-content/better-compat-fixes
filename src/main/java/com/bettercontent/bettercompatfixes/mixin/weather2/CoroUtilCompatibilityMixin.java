package com.bettercontent.bettercompatfixes.mixin.weather2;

import com.bettercontent.bettercompatfixes.compat.Weather2LoadedChunkQueries;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Retain CoroUtil's native seasonal calculation without its second terrain load. */
@Pseudo
@Mixin(targets = "com.corosus.coroutil.util.CoroUtilCompatibility", remap = false)
public abstract class CoroUtilCompatibilityMixin {
    @WrapOperation(method = "getAdjustedTemperature", remap = false, require = 1,
            at = {
                @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;", remap = false),
                @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_204166_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;", remap = false)
            })
    private static Holder<Biome> better_compat_fixes$observeSeasonalBiomeWithoutChunkWait(
            Level level, BlockPos pos, Operation<Holder<Biome>> original) {
        return level instanceof ServerLevel server ? Weather2LoadedChunkQueries.biome(server, pos) : original.call(level, pos);
    }
}
