package com.bettercontent.bettercompatfixes.mixin.weather2;

import com.bettercontent.bettercompatfixes.compat.Weather2LoadedChunkQueries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "weather2.util.WeatherUtilBlock", remap = false)
public abstract class WeatherUtilBlockMixin {
    @Inject(method = "getPrecipitationHeightSafe(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/Heightmap$Types;)Lnet/minecraft/core/BlockPos;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private static void better_compat_fixes$observeLoadedHeightOnly(Level world, BlockPos pos,
            Heightmap.Types type, CallbackInfoReturnable<BlockPos> callback) {
        if (world instanceof ServerLevel server) {
            callback.setReturnValue(Weather2LoadedChunkQueries.precipitationHeight(server, pos, type));
        }
    }
}
