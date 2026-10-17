package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.client.ToggleSneakHandler;
import java.util.function.BooleanSupplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Survival crouch latches only on land; water descent always follows the physical key. */
@Mixin(ToggleKeyMapping.class)
public abstract class SurvivalCrouchToggleMixin extends KeyMapping {
    @Unique private boolean betterContentFixes$wasInWater;

    private SurvivalCrouchToggleMixin() {
        super("better_content.unused", -1, "better_content.unused");
    }

    @Inject(method = {"isDown", "m_90857_"}, at = @At("HEAD"), remap = false)
    private void betterContentFixes$clearMovementLatch(final CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if ((Object) this != minecraft.options.keyShift || minecraft.player == null) return;
        if (minecraft.player.isInWaterOrBubble()) {
            betterContentFixes$wasInWater = true;
            // Bypass ToggleKeyMapping.setDown: false must clear, not merely leave a latch intact.
            super.setDown(minecraft.screen == null
                    && ToggleSneakHandler.physicalKeyDown(minecraft, getKey()));
        } else if (betterContentFixes$wasInWater) {
            betterContentFixes$wasInWater = false;
            super.setDown(false);
        } else if (minecraft.options.toggleCrouch().get()
                && minecraft.gameMode != null
                && minecraft.gameMode.getPlayerMode() == GameType.SURVIVAL
                && (minecraft.options.keyJump.isDown() || minecraft.options.keySprint.isDown()
                    || minecraft.player.isSprinting())) {
            super.setDown(false);
        }
    }
    @Redirect(method = {"setDown", "m_7249_", "isDown", "m_90857_"},
            at = @At(value = "INVOKE",
                    target = "Ljava/util/function/BooleanSupplier;getAsBoolean()Z"),
            require = 2,
            remap = false)
    private boolean betterContentFixes$survivalOnlyCrouchToggle(final BooleanSupplier setting) {
        Minecraft minecraft = Minecraft.getInstance();
        if ((Object) this != minecraft.options.keyShift) return setting.getAsBoolean();
        return setting.getAsBoolean() && minecraft.player != null && minecraft.gameMode != null
                && minecraft.gameMode.getPlayerMode() == GameType.SURVIVAL
                && !minecraft.player.isInWaterOrBubble();
    }
}
