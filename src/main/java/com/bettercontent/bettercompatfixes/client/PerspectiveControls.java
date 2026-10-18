package com.bettercontent.bettercompatfixes.client;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** The existing vanilla perspective binding is retained as the rear-view toggle. */
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PerspectiveControls {
    public static final KeyMapping FRONT = new KeyMapping(
            "key.better_compat_fixes.front_perspective", KeyConflictContext.IN_GAME,
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7,
            "key.categories.misc");

    private PerspectiveControls() {}

    @SubscribeEvent
    public static void registerKeys(final RegisterKeyMappingsEvent event) {
        event.register(FRONT);
    }

    /** Rear wins simultaneous input, so a shared binding never switches twice. */
    public static CameraType requestedView(final boolean rearClicked, final boolean frontClicked) {
        return rearClicked ? CameraType.THIRD_PERSON_BACK
                : frontClicked ? CameraType.THIRD_PERSON_FRONT : null;
    }

    public static CameraType toggle(final CameraType current, final CameraType requested) {
        return current == requested ? CameraType.FIRST_PERSON : requested;
    }
}
