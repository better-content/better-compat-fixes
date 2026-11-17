package com.bettercontent.bettercompatfixes.mixin.sophisticatedbackpacks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sophisticated Core 1.3.21.1676: the magnet directly inserts nearby item entities into storage. */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper", remap = false)
public abstract class SpectatorWornMagnetMixin {
    // Only the player-carried magnet is affected. Placed backpacks and other
    // storage, non-spectator players, and non-magnet upgrades continue as before.
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void betterContentFixes$skipSpectatorMagnet(Entity carrier, Level level, BlockPos pos, CallbackInfo ci) {
        if (!level.isClientSide && carrier instanceof ServerPlayer player && player.isSpectator()) {
            ci.cancel();
        }
    }
}
