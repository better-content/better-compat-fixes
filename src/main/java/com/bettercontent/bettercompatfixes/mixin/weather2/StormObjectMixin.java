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

@Pseudo
@Mixin(targets = "weather2.weathersystem.storm.StormObject", remap = false)
public abstract class StormObjectMixin {
    @WrapOperation(method = {"initFirstTime", "tickProgression"}, remap = false, require = 2,
            // This repository deliberately has no refmap. Resolve exactly one
            // development or production call; never silently miss the server hook.
            at = {
                @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;", remap = false),
                @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_204166_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;", remap = false)
            })
    private Holder<Biome> better_compat_fixes$observeBiomeWithoutChunkWait(Level level, BlockPos pos,
            Operation<Holder<Biome>> original) {
        return level instanceof ServerLevel server ? Weather2LoadedChunkQueries.biome(server, pos) : original.call(level, pos);
    }

    @WrapOperation(method = "tickProgression", remap = false, require = 1,
            at = {
                @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isLoaded(Lnet/minecraft/core/BlockPos;)Z", remap = false),
                @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46749_(Lnet/minecraft/core/BlockPos;)Z", remap = false)
            })
    private boolean better_compat_fixes$observeWaterOnlyInResidentTerrain(Level level, BlockPos pos,
            Operation<Boolean> original) {
        return level instanceof ServerLevel server
                ? server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
                : original.call(level, pos);
    }
}
