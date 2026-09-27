package com.bettercontent.bettercontentfixes.compat.epicfight;

import com.bettercontent.bettercontentfixes.BetterContentFixes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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
        if (!(event.getScreen() instanceof InventoryScreen screen)
                || !ModList.get().isLoaded("epicfight")) return;
        int left = (screen.width - 176) / 2;
        int top = (screen.height - 166) / 2;
        event.addListener(Button.builder(Component.translatable("screen.better_content_fixes.fighting_styles"),
                button -> Minecraft.getInstance().setScreen(new StyleScreen()))
                .bounds(left + 48, Math.max(2, top - 20), 83, 18).build());
    }
}
