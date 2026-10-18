package com.bettercontent.bettercompatfixes.client;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.client.CameraType;
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

}
