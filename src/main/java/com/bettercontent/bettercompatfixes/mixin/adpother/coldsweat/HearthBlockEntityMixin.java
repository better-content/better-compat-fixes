package com.bettercontent.bettercompatfixes.mixin.adpother.coldsweat;

import com.endertech.minecraft.mods.adpother.events.WorldEvents;
import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HearthBlockEntity.class, remap = false)
public abstract class HearthBlockEntityMixin {
    @Unique
    private int betterContentFixes$hotFuelBeforeDrain;

    @Inject(method = "drainFuel", at = @At("HEAD"), require = 1)
    private void betterContentFixes$captureHotFuel(final CallbackInfo ci) {
        betterContentFixes$hotFuelBeforeDrain = ((HearthBlockEntity) (Object) this).getHotFuel();
    }

    @Inject(method = "drainFuel", at = @At("RETURN"), require = 1)
    private void betterContentFixes$emitHotFuelPollution(final CallbackInfo ci) {
        final int consumed = betterContentFixes$hotFuelBeforeDrain
                - ((HearthBlockEntity) (Object) this).getHotFuel();
        if (consumed > 0) {
            WorldEvents.onLavaFuelBurned((BlockEntity) (Object) this, consumed);
        }
    }
}
