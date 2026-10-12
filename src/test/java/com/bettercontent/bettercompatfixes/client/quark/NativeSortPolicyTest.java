package com.bettercontent.bettercompatfixes.client.quark;

import org.junit.jupiter.api.Test;
import static com.bettercontent.bettercompatfixes.client.quark.NativeSortPolicy.Target.*;
import static org.junit.jupiter.api.Assertions.*;

class NativeSortPolicyTest {
    @Test void personalVanillaAndCuriosChooseOnlyNativePlayerInventory() {
        for (int index = 0; index < 36; index++)
            assertEquals(PLAYER, NativeSortPolicy.target(false, true, false, true, index));
        assertEquals(NONE, NativeSortPolicy.target(false, true, false, false, 0));
    }
    @Test void acceptedContainerChoosesExactlyOneSideByNativeSlotOwner() {
        assertEquals(CONTAINER_PLAYER, NativeSortPolicy.target(false, false, true, true, 9));
        assertEquals(CONTAINER, NativeSortPolicy.target(false, false, true, false, 9));
        assertEquals(NONE, NativeSortPolicy.target(false, false, false, true, 9));
        assertEquals(NONE, NativeSortPolicy.target(false, false, false, false, 9));
    }
    @Test void equipmentAndBlockedInputsNeverSelectASort() {
        for (int index : new int[]{-1, 36, 37, 38, 39, 40})
            assertEquals(NONE, NativeSortPolicy.target(false, true, true, true, index));
        for (boolean personal : new boolean[]{false, true})
            for (boolean player : new boolean[]{false, true})
                assertEquals(NONE, NativeSortPolicy.target(true, personal, true, player, 9));
    }
    @Test void actualPointerBoundsRejectStaleHoverAndOutsideControls() {
        assertTrue(NativeSortPolicy.contains(20, 30, 20, 30));
        assertTrue(NativeSortPolicy.contains(35.99, 45.99, 20, 30));
        assertFalse(NativeSortPolicy.contains(36, 30, 20, 30));
        assertFalse(NativeSortPolicy.contains(20, 46, 20, 30));
        assertFalse(NativeSortPolicy.contains(19.99, 30, 20, 30));
        assertFalse(NativeSortPolicy.contains(Double.NaN, 30, 20, 30));
    }
}
