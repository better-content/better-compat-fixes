package com.bettercontent.bettercompatfixes.client.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pure parity checks; a real provider/client fixture is still required before integration. */
class BloodMagicTooltipAdapterTest {
    @Test void harvestOnlyNeedsMiningButMixedTriggersRemainVisible() {
        assertFalse(BloodMagicTooltipAdapter.visible(true, Set.of("combat")));
        assertTrue(BloodMagicTooltipAdapter.visible(true, Set.of("mining")));
        // Attack or use-finish anointments are not harvest-only and remain visible.
        assertTrue(BloodMagicTooltipAdapter.visible(false, Set.of("combat")));
        assertTrue(BloodMagicTooltipAdapter.visible(false, Set.of()));
    }

    @Test void remainingUsesAreClampedWithoutChangingTheOriginalCount() {
        assertEquals(7, BloodMagicTooltipAdapter.remainingUses(10, 3));
        assertEquals(0, BloodMagicTooltipAdapter.remainingUses(3, 10));
        assertEquals(0, BloodMagicTooltipAdapter.remainingUses(0, 0));
    }
}
