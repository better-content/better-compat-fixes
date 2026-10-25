package com.bettercontent.bettercompatfixes.mixin.vibration;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Non-injecting interface anchor; the scoped plugin preserves the three original VS conversions. */
@Mixin(targets = "net.minecraft.world.level.gameevent.vibrations.VibrationSystem$Ticker")
public interface VibrationTickerMixin {
    @Unique
    default void bc$vsVibrationInterfaceAnchor() {}
}
