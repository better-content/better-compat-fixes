package com.bettercontent.bettercompatfixes.compat.epicfight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

final class StyleDiscoveryPolicyTest {
    @Test
    void keysAreItemAndResolvedStyleSpecificAndRejectInvalidInputs() {
        final ResourceLocation sword = new ResourceLocation("example", "sword");
        assertEquals("example:sword|ONE_HAND", StyleDiscoveryPolicy.key(sword, "ONE_HAND"));
        assertFalse(StyleDiscoveryPolicy.key(sword, "TWO_HAND").equals(
                StyleDiscoveryPolicy.key(sword, "ONE_HAND")));
        assertNull(StyleDiscoveryPolicy.key(null, "ONE_HAND"));
        assertNull(StyleDiscoveryPolicy.key(sword, " "));
    }

    @Test
    void onlyFirstObservedCombinationProducesADiscovery() {
        final String first = StyleDiscoveryPolicy.key(new ResourceLocation("example", "sword"), "ONE_HAND");
        final String other = StyleDiscoveryPolicy.key(new ResourceLocation("example", "sword"), "TWO_HAND");
        assertTrue(StyleDiscoveryPolicy.isNew(first, List.of()));
        assertFalse(StyleDiscoveryPolicy.isNew(first, List.of(first)));
        assertTrue(StyleDiscoveryPolicy.isNew(other, List.of(first)));
        assertFalse(StyleDiscoveryPolicy.isNew(null, List.of()));
    }
}
