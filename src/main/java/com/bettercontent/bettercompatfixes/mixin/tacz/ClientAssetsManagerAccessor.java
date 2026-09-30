package com.bettercontent.bettercompatfixes.mixin.tacz;

import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.manager.DisplayManager;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoDisplay;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tacz.guns.client.resource.pojo.display.block.BlockDisplay;
import com.tacz.guns.client.resource.pojo.display.gun.GunDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ClientAssetsManager.class, remap = false)
public interface ClientAssetsManagerAccessor {
    @Accessor("gunDisplay")
    DisplayManager<GunDisplay> betterContentFixes$getGunDisplay();

    @Accessor("ammoDisplay")
    DisplayManager<AmmoDisplay> betterContentFixes$getAmmoDisplay();

    @Accessor("attachmentDisplay")
    DisplayManager<AttachmentDisplay> betterContentFixes$getAttachmentDisplay();

    @Accessor("blockDisplay")
    DisplayManager<BlockDisplay> betterContentFixes$getBlockDisplay();
}
