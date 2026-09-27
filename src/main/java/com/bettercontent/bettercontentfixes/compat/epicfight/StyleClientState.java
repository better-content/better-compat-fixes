package com.bettercontent.bettercontentfixes.compat.epicfight;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bettercontent.bettercontentfixes.BetterContentFixes;

@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, value = Dist.CLIENT)
public final class StyleClientState {
    private static StyleNetwork.Snapshot snapshot = new StyleNetwork.Snapshot(List.of(), "");
    private StyleClientState() {}

    public static StyleNetwork.Snapshot snapshot() { return snapshot; }

    static void receive(StyleNetwork.Snapshot update) {
        snapshot = update;
        if (Minecraft.getInstance().screen instanceof StyleScreen screen) screen.refresh();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        snapshot = new StyleNetwork.Snapshot(List.of(), "");
    }
}
