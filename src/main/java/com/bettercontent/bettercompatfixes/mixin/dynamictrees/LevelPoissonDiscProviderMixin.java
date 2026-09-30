package com.bettercontent.bettercompatfixes.mixin.dynamictrees;

import com.bettercontent.bettercompatfixes.compat.DynamicTreesPoissonDiscSerialization;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(
        targets = "com.ferreusveritas.dynamictrees.systems.poissondisc.LevelPoissonDiscProvider",
        remap = false
)
public abstract class LevelPoissonDiscProviderMixin {
    @WrapMethod(method = "getChunkPoissonData", remap = false)
    private byte[] better_compat_fixes$serializePoissonDataRead(
            final int chunkX,
            final int chunkY,
            final int chunkZ,
            final Operation<byte[]> original
    ) {
        return DynamicTreesPoissonDiscSerialization.call(
                this,
                () -> original.call(chunkX, chunkY, chunkZ)
        );
    }

    @WrapMethod(method = "setChunkPoissonData", remap = false)
    private void better_compat_fixes$serializePoissonDataWrite(
            final int chunkX,
            final int chunkY,
            final int chunkZ,
            final byte[] data,
            final Operation<Void> original
    ) {
        DynamicTreesPoissonDiscSerialization.run(
                this,
                () -> original.call(chunkX, chunkY, chunkZ, data)
        );
    }

    @WrapMethod(method = "unloadChunkPoissonData", remap = false)
    private void better_compat_fixes$serializePoissonDataUnload(
            final int chunkX,
            final int chunkY,
            final int chunkZ,
            final Operation<Void> original
    ) {
        DynamicTreesPoissonDiscSerialization.run(
                this,
                () -> original.call(chunkX, chunkY, chunkZ)
        );
    }
}
