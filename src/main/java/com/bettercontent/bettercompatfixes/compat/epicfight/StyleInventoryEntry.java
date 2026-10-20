package com.bettercontent.bettercompatfixes.compat.epicfight;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, value = Dist.CLIENT)
public final class StyleInventoryEntry {
    private StyleInventoryEntry() {}

    @SubscribeEvent
    public static void addButton(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || !StyleInventoryPolicy.accepts(screen instanceof InventoryScreen,
                    screen.getClass().getName(), ModList.get().isLoaded("epicfight"))) return;
        if (StyleInventoryPolicy.journalHost(ModList.get().isLoaded("better_journal_inventory"),
                screen instanceof InventoryScreen, screen.getClass().getName())) return;
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        event.addListener(Button.builder(Component.translatable("screen.better_compat_fixes.fighting_styles"),
                button -> Minecraft.getInstance().setScreen(new StyleScreen()))
                .bounds(StyleInventoryPolicy.buttonX(left), StyleInventoryPolicy.buttonY(top), 83, 18).build());
    }
}
