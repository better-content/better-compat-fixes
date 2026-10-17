package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Source wiring guards; native in-game movement acceptance remains a separate check. */
final class CrouchToggleResourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/bettercontent/bettercompatfixes");

    @Test void waterDisablesVanillaLatchAndReadsThePhysicalBinding() throws Exception {
        String mixin = Files.readString(ROOT.resolve("mixin/minecraft/SurvivalCrouchToggleMixin.java"));
        assertTrue(mixin.contains("&& !minecraft.player.isInWaterOrBubble()"));
        assertTrue(mixin.contains("super.setDown(minecraft.screen == null"));
        assertTrue(mixin.contains("ToggleSneakHandler.physicalKeyDown(minecraft, getKey())"));
        assertTrue(mixin.contains("else if (betterContentFixes$wasInWater)"),
                "A held descent key must not become a land latch on leaving water");
        assertTrue(mixin.contains("super.setDown(false)"),
                "ToggleKeyMapping.setDown(false) does not clear a latched toggle");
        assertFalse(mixin.contains("options.toggleCrouch().set("));
        String physical = Files.readString(ROOT.resolve("client/ToggleSneakHandler.java"));
        assertTrue(physical.contains("InputConstants.Type.KEYSYM"));
        assertTrue(physical.contains("InputConstants.Type.MOUSE"));
    }

    @Test void jumpAndSprintIntentClearTheLatchWithoutReplacingSprintMapping() throws Exception {
        String mixin = Files.readString(ROOT.resolve("mixin/minecraft/SurvivalCrouchToggleMixin.java"));
        assertTrue(mixin.contains("@Inject(method = {\"isDown\", \"m_90857_\"}, at = @At(\"HEAD\")"));
        assertTrue(mixin.contains("(Object) this != minecraft.options.keyShift"));
        assertTrue(mixin.contains("minecraft.options.toggleCrouch().get()"));
        assertTrue(mixin.contains("GameType.SURVIVAL"));
        assertTrue(mixin.contains("minecraft.options.keyJump.isDown()"));
        assertTrue(mixin.contains("minecraft.options.keySprint.isDown()"),
                "Waiting for isSprinting alone deadlocks behind crouch's sprint guard");
        assertTrue(mixin.contains("minecraft.player.isSprinting()"));
        assertFalse(mixin.contains("keySprint.setDown"));
    }
}
