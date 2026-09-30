package com.bettercontent.bettercompatfixes.mixin.sophisticatedbackpacks;

import com.bettercontent.bettercompatfixes.compat.SophisticatedBackpackJukeboxAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Disables only legacy jukebox upgrades hosted by Sophisticated Backpacks. */
@Mixin(value = JukeboxUpgradeWrapper.class, remap = false)
public abstract class JukeboxUpgradeWrapperMixin {

    @Shadow
    public abstract boolean isPlaying();

    @Shadow
    public abstract void stop(LivingEntity entity);

    private boolean betterContent$isBackpackJukebox() {
        return (Object) this instanceof SophisticatedBackpackJukeboxAccess access
                && access.betterContent$isBackpackJukebox();
    }

    @Inject(method = "isEnabled", at = @At("HEAD"), cancellable = true)
    private void betterContent$disableBackpackJukebox(final CallbackInfoReturnable<Boolean> callback) {
        if (betterContent$isBackpackJukebox()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "play(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), cancellable = true)
    private void betterContent$rejectBlockPlayback(final Level level, final BlockPos position, final CallbackInfo callback) {
        if (betterContent$isBackpackJukebox()) {
            callback.cancel();
        }
    }

    @Inject(method = "play(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void betterContent$rejectEntityPlayback(final Entity entity, final CallbackInfo callback) {
        if (betterContent$isBackpackJukebox()) {
            callback.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void betterContent$stopLegacyPlayback(final Entity entity, final Level level, final BlockPos position, final CallbackInfo callback) {
        if (!betterContent$isBackpackJukebox()) {
            return;
        }
        if (isPlaying() && entity instanceof LivingEntity livingEntity) {
            stop(livingEntity);
        }
        callback.cancel();
    }
}
