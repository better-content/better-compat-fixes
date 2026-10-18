package com.bettercontent.bettercompatfixes.client;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Samples physical arrows, never movement mappings or synthetic key presses. */
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, value = Dist.CLIENT)
public final class ArrowDoubleTapDodgeClient {
    private static final DirectionalDoubleTapTracker TRACKER = new DirectionalDoubleTapTracker();
    private static LocalPlayer trackedPlayer;
    private static ClientLevel trackedLevel;
    private static Vec3 pendingMove;

    private ArrowDoubleTapDodgeClient() {
    }

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        // ParCool evaluates canStart during the player tick between START and END.
        pendingMove = null;
        if (event.phase != TickEvent.Phase.START) return;
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != trackedPlayer || minecraft.level != trackedLevel) {
            TRACKER.reset();
            trackedPlayer = minecraft.player;
            trackedLevel = minecraft.level;
        }
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || minecraft.isPaused() || !minecraft.isWindowActive()
                || !minecraft.player.isAlive() || minecraft.player.isSpectator()
                || minecraft.player.isPassenger() || minecraft.player.getAbilities().flying
                || minecraft.player.isShiftKeyDown() || minecraft.player.isCrouching()
                || minecraft.options.keyShift.isDown()) {
            TRACKER.reset();
            return;
        }
        final long window = minecraft.getWindow().getWindow();
        if (!InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)) {
            TRACKER.reset();
            return;
        }
        final boolean forward = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_UP);
        final boolean backward = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_DOWN);
        final boolean left = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT);
        final boolean right = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT);
        if (TRACKER.update(forward, backward, left, right, 7)) {
            final float z = DirectionalDoubleTapTracker.axisImpulse(forward, backward);
            final float x = DirectionalDoubleTapTracker.axisImpulse(left, right);
            if (x != 0 || z != 0) pendingMove = new Vec3(x, 0, z).normalize();
        }
    }

    /** One-tick request; native ParCool still owns eligibility, stamina, cooldown and networking. */
    public static Vec3 pendingMove() {
        return pendingMove;
    }
}
