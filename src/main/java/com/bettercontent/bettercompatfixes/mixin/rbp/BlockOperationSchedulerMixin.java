package com.bettercontent.bettercompatfixes.mixin.rbp;

import com.bettercontent.bettercompatfixes.compat.RespawnMarkerPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xbigellx.rbp.internal.physics.BlockOperation;
import xbigellx.rbp.internal.physics.engine.PhysicsEngineBehaviour;
import xbigellx.realisticphysics.internal.level.block.RPBlockContext;

/** Pinned RBP's common FALL/BREAK admission, including placement, neighbors and chunk analysis. */
@Mixin(targets = "xbigellx.rbp.internal.physics.BlockOperationScheduler", remap = false)
public abstract class BlockOperationSchedulerMixin {
    @Inject(
            method = "schedule(Lxbigellx/rbp/internal/physics/BlockOperation;Lxbigellx/realisticphysics/internal/level/block/RPBlockContext;Lxbigellx/rbp/internal/physics/engine/PhysicsEngineBehaviour;)V",
            at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void betterContentFixes$excludeRespawnMarker(
            final BlockOperation operation,
            final RPBlockContext block,
            final PhysicsEngineBehaviour behaviour,
            final CallbackInfo callback
    ) {
        if (RespawnMarkerPhysics.isExcluded(block.blockState())) callback.cancel();
    }
}
