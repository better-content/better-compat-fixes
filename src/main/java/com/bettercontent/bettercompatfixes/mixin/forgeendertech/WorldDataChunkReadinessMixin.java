package com.bettercontent.bettercompatfixes.mixin.forgeendertech;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Objects;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** ChunkFullyLoadedEvent must observe a resident FULL chunk, not await a loading ticket. */
@Pseudo
@Mixin(targets = "com.endertech.minecraft.forge.world.GameWorld$WorldData", remap = false)
public abstract class WorldDataChunkReadinessMixin {
    @WrapOperation(method = "onLevelTick", remap = false, require = 1,
            at = {
                @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkSource;hasChunk(II)Z", remap = false),
                @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkSource;m_5563_(II)Z", remap = false)
            })
    private static boolean better_compat_fixes$requireResidentFullChunk(ChunkSource source, int x, int z,
            Operation<Boolean> original) {
        return source instanceof ServerChunkCache server ? server.getChunkNow(x, z) != null : original.call(source, x, z);
    }

    @WrapOperation(method = "onLevelTick", remap = false, require = 1,
            at = {
                @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;", remap = false),
                @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_6325_(II)Lnet/minecraft/world/level/chunk/LevelChunk;", remap = false)
            })
    private static LevelChunk better_compat_fixes$observeProvenResidentChunk(ServerLevel level, int x, int z,
            Operation<LevelChunk> original) {
        // The native readiness branch and this lookup run consecutively on the
        // server thread, with only iterator removal between them. Never issue a
        // chunk request here; an invariant violation must be visible, not hang.
        return Objects.requireNonNull(level.getChunkSource().getChunkNow(x, z),
                "ForgeEndertech ready chunk disappeared within its server-thread notification");
    }
}
