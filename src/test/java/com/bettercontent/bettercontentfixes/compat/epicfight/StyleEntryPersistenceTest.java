package com.bettercontent.bettercontentfixes.compat.epicfight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class StyleEntryPersistenceTest {
    @Test
    void restoresObservedToolComboAfterCatalogueRebuild() {
        var motions = List.of(new ResourceLocation("epicfight", "tool_first"),
                new ResourceLocation("epicfight", "tool_second"),
                new ResourceLocation("epicfight", "tool_finisher"));
        var original = new StyleCatalogue.Entry(StyleCatalogue.idFor(motions), "Tinker longsword",
                motions, Set.of(new ResourceLocation("tconstruct", "longsword")));

        var saved = StyleState.encodeEntry(original);

        assertEquals(original, StyleState.decodeEntry(original.id(), saved));
        assertNull(StyleState.decodeEntry("wrong", saved));
    }
}
