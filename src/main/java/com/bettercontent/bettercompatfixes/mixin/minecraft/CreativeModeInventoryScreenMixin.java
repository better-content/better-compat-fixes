package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.CreativeModeTabRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Redirect(
            // This project intentionally ships without a refmap. Keep both the development and
            // production names so the same client-only redirect resolves in either namespace.
            method = {"init", "m_7856_"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/common/CreativeModeTabRegistry;getSortedCreativeModeTabs()Ljava/util/List;"),
            require = 1,
            remap = false)
    private List<CreativeModeTab> betterContentFixes$visibleCreativeTabs() {
        final List<CreativeModeTab> tabs = CreativeModeTabRegistry.getSortedCreativeModeTabs();
        if (!BcFixesConfig.hideCreativeInventorySearchTab()) {
            return tabs;
        }
        return tabs.stream()
                .filter(tab -> tab != CreativeModeTabs.searchTab())
                .toList();
    }
}
