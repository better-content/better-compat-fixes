package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.compat.PendingChunkReadSnapshots;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Never expose an in-flight save's mutable root to asynchronous chunk upgrading. */
@Mixin(IOWorker.class)
public abstract class IOWorkerPendingReadMixin {
    // Only the pending-write branch: disk reads already create independent tags.
    // Copy on the mailbox thread, BEFORE the native future publishes the result.
    // Keep the original future, cancellation/exception behavior, deletion sentinel,
    // disk-read path and write ordering; no blanket store/read copies or locks.
    @ModifyExpressionValue(
            method = "lambda$loadAsync$4(Lnet/minecraft/world/level/ChunkPos;)Lcom/mojang/datafixers/util/Either;",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/chunk/storage/IOWorker$PendingStore;data:Lnet/minecraft/nbt/CompoundTag;"),
            require = 1, allow = 1)
    private CompoundTag better_compat_fixes$detachPendingRead(CompoundTag pending) {
        return PendingChunkReadSnapshots.detach(pending);
    }
}
