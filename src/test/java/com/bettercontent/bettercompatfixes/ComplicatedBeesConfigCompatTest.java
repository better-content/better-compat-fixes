package com.bettercontent.bettercompatfixes;

import com.bettercontent.bettercompatfixes.compat.ComplicatedBeesConfigCompat;
import com.electronwill.nightconfig.core.CommentedConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplicatedBeesConfigCompatTest {
    @Test
    void convertsOnlyTheComplicatedBeesResearchBonusToFloat() {
        final CommentedConfig config = CommentedConfig.inMemory();
        config.set(List.of("production_cap", "productivityCap"), 50_000);
        config.set(List.of("research", "researchBonus"), 0.2D);

        assertTrue(ComplicatedBeesConfigCompat.normalizeResearchBonus(config));
        assertInstanceOf(Float.class, config.get(List.of("research", "researchBonus")));
        assertEquals(0.2F, config.<Float>get(List.of("research", "researchBonus")));
    }

    @Test
    void ignoresOtherForgeConfigsAndAlreadyNormalizedValues() {
        final CommentedConfig unrelated = CommentedConfig.inMemory();
        unrelated.set(List.of("research", "researchBonus"), 0.2D);
        assertFalse(ComplicatedBeesConfigCompat.normalizeResearchBonus(unrelated));

        final CommentedConfig normalized = CommentedConfig.inMemory();
        normalized.set(List.of("production_cap", "productivityCap"), 50_000);
        normalized.set(List.of("research", "researchBonus"), 0.2F);
        assertFalse(ComplicatedBeesConfigCompat.normalizeResearchBonus(normalized));
    }
}
