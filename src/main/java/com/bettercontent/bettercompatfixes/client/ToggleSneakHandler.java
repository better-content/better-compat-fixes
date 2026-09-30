package com.bettercontent.bettercompatfixes.client;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

/**
 * Toggles the vanilla sneak mapping instead of replacing player movement. This preserves
 * crawling, combat movement, and vehicle input ownership in their respective mods.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ToggleSneakHandler {
    private static boolean toggled;
    private static boolean physicalKeyWasDown;
    private static boolean wasSurvival;

    private ToggleSneakHandler() {}

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.START) {
            if (toggled && minecraft.player != null && minecraft.screen == null && minecraft.options.keyJump.isDown()) {
                toggled = false;
                minecraft.options.keyShift.setDown(false);
            }
            return;
        }
        if (event.phase != TickEvent.Phase.END) return;

        final boolean survival = minecraft.player != null && minecraft.gameMode != null
                && minecraft.gameMode.getPlayerMode() == GameType.SURVIVAL;
        if (!survival) {
            toggled = false;
            physicalKeyWasDown = false;
            if (wasSurvival) {
                KeyMapping sneak = minecraft.options.keyShift;
                sneak.setDown(minecraft.player != null && minecraft.screen == null
                        && physicalKeyDown(minecraft, sneak.getKey()));
            }
            wasSurvival = false;
            notifyDynamicHudCancel();
            return;
        }
        wasSurvival = true;

        if (!BcFixesConfig.toggleSneak()) {
            toggled = false;
            physicalKeyWasDown = false;
            return;
        }

        if (minecraft.player == null || minecraft.screen != null) {
            physicalKeyWasDown = false;
            notifyDynamicHudCancel();
            return;
        }
        final KeyMapping sneak = minecraft.options.keyShift;
        final InputConstants.Key key = sneak.getKey();
        final boolean physicalKeyDown = physicalKeyDown(minecraft, key);
        notifyDynamicHud(physicalKeyDown);
        if (physicalKeyDown && !physicalKeyWasDown) {
            toggled = !toggled;
        }
        physicalKeyWasDown = physicalKeyDown;

        // Do not keep crouch active while another system owns mounted movement.
        if (minecraft.player.isPassenger() || minecraft.options.keyJump.isDown()) {
            toggled = false;
        }
        sneak.setDown(toggled);
    }

    private static boolean physicalKeyDown(final Minecraft minecraft, final InputConstants.Key key) {
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(minecraft.getWindow().getWindow(), key.getValue()) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), key.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    private static void notifyDynamicHud(final boolean down) {
        if (ModList.get().isLoaded("better_survival_hud")) {
            DynamicHudSneakIntegration.setPhysicalSneakDown(down);
        }
    }

    private static void notifyDynamicHudCancel() {
        if (ModList.get().isLoaded("better_survival_hud")) {
            DynamicHudSneakIntegration.cancelPhysicalSneak();
        }
    }
}
