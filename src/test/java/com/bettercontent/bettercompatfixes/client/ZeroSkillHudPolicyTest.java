package com.bettercontent.bettercompatfixes.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ZeroSkillHudPolicyTest {
    @Test
    void roundedCountersDisappearAtZeroAndReturnAtOne() {
        assertFalse(ZeroSkillHudPolicy.roundedPositive(0.0F));
        assertFalse(ZeroSkillHudPolicy.roundedPositive(0.49F));
        assertTrue(ZeroSkillHudPolicy.roundedPositive(0.5F));
        assertTrue(ZeroSkillHudPolicy.roundedPositive(1.0F));
        assertFalse(ZeroSkillHudPolicy.roundedPositive(Float.NaN));
    }
}
