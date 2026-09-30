package com.bettercontent.bettercompatfixes.client.settings;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(
        modid = BetterContentFixes.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class OptionsModSettingsEntry {
    private static final int BUTTON_WIDTH = 112;
    private static final int BUTTON_HEIGHT = 20;
    private static final int EDGE_PADDING = 5;

    private OptionsModSettingsEntry() {
    }

    @SubscribeEvent
    public static void addModSettingsButton(final ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof OptionsScreen options)
                || !ModList.get().isLoaded("configured")) {
            return;
        }

        int x = Math.max(EDGE_PADDING, options.width - BUTTON_WIDTH - EDGE_PADDING);
        event.addListener(net.minecraft.client.gui.components.Button.builder(
                        Component.translatable("options.better_compat_fixes.mod_settings"),
                        button -> Minecraft.getInstance().setScreen(new ModListScreen(options)))
                .bounds(x, EDGE_PADDING, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }
}
