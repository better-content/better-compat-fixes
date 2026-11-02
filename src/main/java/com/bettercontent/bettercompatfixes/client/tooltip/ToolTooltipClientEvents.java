package com.bettercontent.bettercompatfixes.client.tooltip;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

@Mod.EventBusSubscriber(modid = "better_compat_fixes", value = Dist.CLIENT)
public final class ToolTooltipClientEvents {
    private ToolTooltipClientEvents() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void gather(RenderTooltipEvent.GatherComponents event) {
        if (!ToolTooltipOwner.owns(event.getItemStack())) return;
        int width = Math.max(40, Math.min(ToolTooltipRenderer.TARGET_WIDTH, event.getScreenWidth() - 24));
        if (event.getMaxWidth() > 0) width = Math.min(width, event.getMaxWidth());
        event.setMaxWidth(width);
        // Quark's components are outside the text-list return boundary. Do not remove other mods' visuals.
        event.getTooltipElements().removeIf(element -> element.right().map(component ->
                component.getClass().getName().startsWith("org.violetmoon.quark.content.client.tooltip.AttributeTooltips$")).orElse(false));
    }

    @Mod.EventBusSubscriber(modid = "better_compat_fixes", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        private Reload() {}
        @SubscribeEvent
        public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener)ToolTooltipCatalogue::reload);
        }
    }
}
