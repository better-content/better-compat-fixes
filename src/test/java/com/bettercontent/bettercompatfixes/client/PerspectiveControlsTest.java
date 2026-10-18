package com.bettercontent.bettercompatfixes.client;

import static org.junit.jupiter.api.Assertions.*;

import com.bettercontent.bettercompatfixes.mixin.minecraft.PerspectiveControlsMixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class PerspectiveControlsTest {
    @Test
    void bothButtonsToggleTheirOwnViewAndSwitchDirectlyFromTheOtherView() {
        for (var current : CameraType.values()) {
            for (var requested : new CameraType[]{CameraType.THIRD_PERSON_BACK, CameraType.THIRD_PERSON_FRONT}) {
                assertEquals(current == requested ? CameraType.FIRST_PERSON : requested,
                        PerspectiveControls.toggle(current, requested));
            }
        }
    }

    @Test
    void noInputDoesNothingAndRearWinsSimultaneousInput() {
        assertNull(PerspectiveControls.requestedView(false, false));
        assertEquals(CameraType.THIRD_PERSON_BACK, PerspectiveControls.requestedView(true, false));
        assertEquals(CameraType.THIRD_PERSON_FRONT, PerspectiveControls.requestedView(false, true));
        assertEquals(CameraType.THIRD_PERSON_BACK, PerspectiveControls.requestedView(true, true));
    }

    @Test
    void frontIsAnInGameF7BindingAndDoesNotReplaceVanillasRearMapping() {
        assertEquals(GLFW.GLFW_KEY_F7, PerspectiveControls.FRONT.getDefaultKey().getValue());
        assertEquals(KeyConflictContext.IN_GAME, PerspectiveControls.FRONT.getKeyConflictContext());
        assertEquals("key.better_compat_fixes.front_perspective", PerspectiveControls.FRONT.getName());
    }

    @Test
    void wrapperConsumesBothQueuesAndUsesTheSelectedViewWithoutNativeCycling() throws Exception {
        var mixin = new PerspectiveControlsMixin() {};
        var consume = PerspectiveControlsMixin.class.getDeclaredMethod(
                "better_compat_fixes$consumePerspectiveInput", KeyMapping.class, Operation.class);
        var toggle = PerspectiveControlsMixin.class.getDeclaredMethod(
                "better_compat_fixes$toggleRequestedPerspective", CameraType.class, Operation.class);
        consume.setAccessible(true);
        toggle.setAccessible(true);
        Operation<CameraType> mustNotCycle = args -> { throw new AssertionError("Native cycle must not run"); };
        Operation<Boolean> noRear = args -> false;
        Operation<Boolean> rear = args -> true;
        // Seed only the native click queue: OS/modifier polling requires a real client window.
        var clickCount = KeyMapping.class.getDeclaredField("clickCount");
        clickCount.setAccessible(true);
        clickCount.setInt(PerspectiveControls.FRONT, 1);
        assertEquals(true, consume.invoke(mixin, PerspectiveControls.FRONT, noRear));
        assertEquals(CameraType.THIRD_PERSON_FRONT,
                toggle.invoke(mixin, CameraType.THIRD_PERSON_BACK, mustNotCycle));
        assertEquals(false, consume.invoke(mixin, PerspectiveControls.FRONT, noRear));
        clickCount.setInt(PerspectiveControls.FRONT, 1);
        assertEquals(true, consume.invoke(mixin, PerspectiveControls.FRONT, rear));
        assertEquals(CameraType.FIRST_PERSON,
                toggle.invoke(mixin, CameraType.THIRD_PERSON_BACK, mustNotCycle));
        assertFalse(PerspectiveControls.FRONT.consumeClick(), "Simultaneous front click must not survive");
    }
}
