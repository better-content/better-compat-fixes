package com.bettercontent.bettercompatfixes.mixin.create;

import com.bettercontent.bettercompatfixes.compat.WaterWheelBiomePolicy;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies to both standard and large wheels through Create's shared flow-score implementation. */
@Mixin(targets = "com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity", remap = false)
public abstract class WaterWheelBlockEntityMixin {
    @Shadow
    public abstract void setFlowScoreAndUpdate(int score);

    @Inject(method = "determineAndApplyFlowScore", at = @At("HEAD"), cancellable = true)
    private void betterContentFixes$denyGenerationOutsideRivers(final CallbackInfo callback) {
        final BlockEntity blockEntity = (BlockEntity) (Object) this;
        if (blockEntity.getLevel() != null
                && !WaterWheelBiomePolicy.allowsGeneration(blockEntity.getLevel(), blockEntity.getBlockPos())) {
            setFlowScoreAndUpdate(0);
            callback.cancel();
        }
    }
}
