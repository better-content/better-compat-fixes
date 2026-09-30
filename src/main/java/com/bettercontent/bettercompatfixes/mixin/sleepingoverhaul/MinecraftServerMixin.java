package com.bettercontent.bettercompatfixes.mixin.sleepingoverhaul;

import com.bettercontent.bettercompatfixes.compat.sleeping.SleepThreadEpisodes;
import com.bettercontent.bettercompatfixes.compat.sleeping.SleepDangerInterruption;
import com.bettercontent.bettercompatfixes.compat.sleeping.TimelapsePacer;
import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import github.cosmicdan.sleepingoverhaul.SleepingOverhaul;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin {
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void betterContent$interruptTimelapseForImmediateDanger(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        SleepDangerInterruption.interruptIfImmediateDanger((MinecraftServer) (Object) this);
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void betterContent$paceSleepingOverhaulTimelapse(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        final boolean active = SleepingOverhaul.serverState != null
                && SleepingOverhaul.serverState.isTimelapseActive();
        SleepThreadEpisodes.update((MinecraftServer) (Object) this, active);
        TimelapsePacer.pace(
                BcFixesConfig.sleepingOverhaulPaceTimelapse() && active,
                BcFixesConfig.sleepingOverhaulTargetTicksPerSecond());
    }
}
