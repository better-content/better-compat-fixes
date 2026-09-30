package com.bettercontent.bettercompatfixes.compat;

import com.electronwill.nightconfig.core.CommentedConfig;

import java.util.List;

/** Normalizes the one TOML numeric type that Complicated Bees declares as a Java Float. */
public final class ComplicatedBeesConfigCompat {
    private static final List<String> PRODUCTIVITY_CAP = List.of("production_cap", "productivityCap");
    private static final List<String> RESEARCH_BONUS = List.of("research", "researchBonus");

    private ComplicatedBeesConfigCompat() {
    }

    public static boolean normalizeResearchBonus(final CommentedConfig config) {
        if (!(config.get(PRODUCTIVITY_CAP) instanceof Integer)
                || !(config.get(RESEARCH_BONUS) instanceof Double bonus)) {
            return false;
        }
        config.set(RESEARCH_BONUS, bonus.floatValue());
        return true;
    }
}
