package com.bettercontent.bettercompatfixes.mixin.explosionoverhaul;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.vinlanx.explosionoverhaul.PacketHandler;
import com.vinlanx.explosionoverhaul.ScanControlPacket;
import com.vinlanx.explosionoverhaul.ScanInfoHUD;
import com.vinlanx.explosionoverhaul.ScanPromptHUD;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.vinlanx.explosionoverhaul.ScanPromptPacket", remap = false)
public abstract class ScanPromptPacketMixin {
    @Redirect(
            method = "lambda$handle$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/vinlanx/explosionoverhaul/ScanPromptHUD;setVisible(Z)V"
            )
    )
    private static void betterContentFixes$acceptScanWithoutPrompt(final boolean showPrompt) {
        if (!BcFixesConfig.explosionOverhaulAutoAcceptScanPrompts()) {
            ScanPromptHUD.setVisible(showPrompt);
            return;
        }

        ScanPromptHUD.setVisible(false);
        ScanInfoHUD.setVisible(false);
        if (showPrompt) {
            PacketHandler.INSTANCE.sendToServer(new ScanControlPacket(true));
        }
    }
}
