package com.bettercontent.bettercompatfixes.mixin.explosionoverhaul;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.vinlanx.explosionoverhaul.PacketHandler;
import com.vinlanx.explosionoverhaul.ScanLoadControlPacket;
import com.vinlanx.explosionoverhaul.ScanLoadInfoHUD;
import com.vinlanx.explosionoverhaul.ScanLoadPromptHUD;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.vinlanx.explosionoverhaul.ScanLoadPromptPacket", remap = false)
public abstract class ScanLoadPromptPacketMixin {
    @Redirect(
            method = "lambda$handle$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/vinlanx/explosionoverhaul/ScanLoadPromptHUD;setVisible(Z)V"
            )
    )
    private static void betterContentFixes$loadExistingWithoutPrompt(final boolean showPrompt) {
        if (!BcFixesConfig.explosionOverhaulAutoAcceptScanPrompts()) {
            ScanLoadPromptHUD.setVisible(showPrompt);
            return;
        }

        ScanLoadPromptHUD.setVisible(false);
        ScanLoadInfoHUD.setVisible(false);
        if (showPrompt) {
            PacketHandler.INSTANCE.sendToServer(new ScanLoadControlPacket(true));
        }
    }
}
