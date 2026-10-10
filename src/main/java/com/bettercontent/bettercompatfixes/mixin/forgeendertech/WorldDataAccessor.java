package com.bettercontent.bettercompatfixes.mixin.forgeendertech;

import java.util.Map;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Typed native pending-notification state for regression fixtures; no copied scheduler. */
@Pseudo
@Mixin(targets = "com.endertech.minecraft.forge.world.GameWorld$WorldData", remap = false)
public interface WorldDataAccessor {
    @Accessor("freshlyLoadedChunks")
    Map<ChunkPos, Long> better_compat_fixes$getFreshlyLoadedChunks();
}
