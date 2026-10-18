package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.compat.ChoppingParticles;
import com.bettercontent.bettercompatfixes.compat.ChoppingProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class ChoppingProgressMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private float destroyProgress;
    @Unique private final ChoppingProgress bc$chips = new ChoppingProgress();
    @Unique private net.minecraft.world.level.block.state.BlockState bc$material;

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), require = 1)
    private void bc$newTarget(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> ci) {
        bc$chips.reset();
        bc$material = minecraft.level == null ? null : minecraft.level.getBlockState(pos);
    }

    @Inject(method = "stopDestroyBlock", at = @At("HEAD"), require = 1)
    private void bc$stop(CallbackInfo ci) { bc$chips.reset(); }

    // First write is the real hardness/tool-adjusted increment, before completion resets it to zero.
    @Inject(method = "continueDestroyBlock", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyProgress:F",
            opcode = 181, ordinal = 0, shift = At.Shift.AFTER), require = 1)
    private void bc$progress(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> ci) {
        if (minecraft.level == null) { bc$chips.reset(); return; }
        var state = minecraft.level.getBlockState(pos);
        if (state != bc$material) {
            bc$chips.reset();
            bc$material = null; // Wait for a fresh native start; do not charge old completion to new material.
            return;
        }
        if (ChoppingParticles.isWood(state)) {
            // This hook runs once per native mining tick (20 Hz), not once per rendered frame.
            ChoppingParticles.emit(minecraft, pos, face,
                    bc$chips.advance(destroyProgress, ChoppingProgress.NATIVE_TICK_SECONDS));
        } else bc$chips.reset();
    }
}
