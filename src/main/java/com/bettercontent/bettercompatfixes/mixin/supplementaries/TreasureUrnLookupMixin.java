package com.bettercontent.bettercompatfixes.mixin.supplementaries;

import com.bettercontent.bettercompatfixes.compat.TreasureUrnPending;
import com.bettercontent.bettercompatfixes.compat.TreasureUrnPredicate;
import com.bettercontent.bettercompatfixes.compat.TreasureUrnStates;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Never edits pending NBT, decoder, block-state predicate, or native postprocessing lifecycle. */
@Mixin(LevelChunk.class)
public abstract class TreasureUrnLookupMixin {
    @Inject(method = "getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            at = @At("HEAD"), cancellable = true, require = 1, expect = 1, allow = 1)
    private void betterContent$skipStatelessPromotion(BlockPos pos, LevelChunk.EntityCreationType creation,
                                                      CallbackInfoReturnable<BlockEntity> cir) {
        LevelChunk chunk = (LevelChunk) (Object) this;
        if (chunk.getClass() != LevelChunk.class) return; // Unknown overridden accessors must keep the native path.
        boolean live = chunk.getBlockEntities().containsKey(pos); // Includes removed entries: never invoke materializing lookup.
        if (live || !TreasureUrnStates.isStateless(chunk.getBlockState(pos))) return;
        if (TreasureUrnPredicate.skipLookup(true, live, TreasureUrnPending.absentOrMinimal(chunk.getBlockEntityNbt(pos), pos)))
            cir.setReturnValue(null);
    }
}
