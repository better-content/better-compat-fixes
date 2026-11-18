package com.bettercontent.bettercompatfixes.client.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Presentation parity only; native spell validity still needs an approved provider fixture. */
class ArsReactiveTooltipAdapterTest {
    @Test void reactiveSpellRetainsConditionalSemanticIdentityAndManaCaveat() {
        var effect = ArsReactiveTooltipAdapter.describe("Touch → Heal");
        assertEquals("ars_nouveau:reactive", effect.semanticId());
        assertEquals(ToolTooltipEffect.Category.CONDITIONAL, effect.category());
        assertTrue(effect.requiredCapabilities().isEmpty());
        assertFalse(effect.representedInStats());
        assertTrue(effect.visible(Set.of()));
        assertTrue(effect.text().getString().contains("Touch → Heal"));
        assertTrue(effect.text().getString().contains("requires mana"));
    }
}
