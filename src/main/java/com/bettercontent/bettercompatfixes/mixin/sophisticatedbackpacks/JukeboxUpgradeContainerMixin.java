package com.bettercontent.bettercompatfixes.mixin.sophisticatedbackpacks;

import com.bettercontent.bettercompatfixes.compat.SophisticatedBackpackJukeboxAccess;
import net.minecraft.nbt.CompoundTag;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the shared Core jukebox menu inert only when its wrapper belongs to a backpack. */
@Mixin(value = JukeboxUpgradeContainer.class, remap = false)
public abstract class JukeboxUpgradeContainerMixin {
    @Inject(method = "handleMessage", at = @At("HEAD"), cancellable = true)
    private void betterContent$rejectBackpackJukeboxMenu(final CompoundTag data, final CallbackInfo callback) {
        if (((UpgradeContainerBase<?, ?>) (Object) this).getUpgradeWrapper()
                instanceof SophisticatedBackpackJukeboxAccess access
                && access.betterContent$isBackpackJukebox()) {
            callback.cancel();
        }
    }
}
