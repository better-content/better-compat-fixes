package com.bettercontent.bettercompatfixes.mixin.sophisticatedbackpacks;

import com.bettercontent.bettercompatfixes.compat.SophisticatedBackpackJukeboxAccess;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Supplies the typed storage-owner check used by shared Core upgrade wrappers. */
@Mixin(value = UpgradeWrapperBase.class, remap = false)
public abstract class UpgradeWrapperBaseMixin implements SophisticatedBackpackJukeboxAccess {
    @Shadow
    protected IStorageWrapper storageWrapper;

    @Override
    public boolean betterContent$isBackpackJukebox() {
        return storageWrapper instanceof BackpackWrapper;
    }
}
