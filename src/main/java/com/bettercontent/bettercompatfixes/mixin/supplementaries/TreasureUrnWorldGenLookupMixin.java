package com.bettercontent.bettercompatfixes.mixin.supplementaries;

import com.bettercontent.bettercompatfixes.compat.TreasureUrnPending;
import com.bettercontent.bettercompatfixes.compat.TreasureUrnPredicate;
import com.bettercontent.bettercompatfixes.compat.TreasureUrnStates;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cached native region lookup only. No LevelChunk.getBlockEntity call or exception swallowing. */
@Mixin(WorldGenRegion.class)
public abstract class TreasureUrnWorldGenLookupMixin {
    @Inject(method = "getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            at = @At("HEAD"), cancellable = true, require = 1, expect = 1, allow = 1)
    private void betterContent$skipStatelessRegionPromotion(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
        WorldGenRegion region = (WorldGenRegion) (Object) this;
        if (region.getClass() != WorldGenRegion.class) return;
        ChunkAccess chunk = region.getChunk(pos); // Same cached native first operation; original out-of-bounds/status errors propagate.
        if (chunk.getClass() == ImposterProtoChunk.class) {
            LevelChunk wrapped = ((ImposterProtoChunk) chunk).getWrapped(); // Pinned native direct field read only.
            if (wrapped.getClass() != LevelChunk.class) return;
            chunk = wrapped;
        }
        boolean live;
        if (chunk.getClass() == ProtoChunk.class) live = ((ProtoChunk) chunk).getBlockEntities().containsKey(pos);
        else if (chunk.getClass() == LevelChunk.class) live = ((LevelChunk) chunk).getBlockEntities().containsKey(pos);
        else return; // Unknown subclasses may materialize on access; do not intercept them.
        if (live || !TreasureUrnStates.isStateless(chunk.getBlockState(pos))) return;
        if (TreasureUrnPredicate.skipLookup(true, live, TreasureUrnPending.absentOrMinimal(chunk.getBlockEntityNbt(pos), pos)))
            cir.setReturnValue(null);
    }
}
