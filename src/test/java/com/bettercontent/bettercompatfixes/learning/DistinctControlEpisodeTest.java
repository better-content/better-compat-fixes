package com.bettercontent.bettercompatfixes.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class DistinctControlEpisodeTest {
    @Test
    void startsWhenNoPersistedEpisodeExists() {
        assertEquals(
                DistinctControlEpisode.Transition.FIRST,
                DistinctControlEpisode.advance("", "", "parcool:dodge"));
    }

    @Test
    void restartsWhenPersistedCorrelationIsMalformed() {
        assertEquals(
                DistinctControlEpisode.Transition.FIRST,
                DistinctControlEpisode.advance("not printable\n", "parcool:dodge", "parcool:vault"));
        assertEquals(
                DistinctControlEpisode.Transition.FIRST,
                DistinctControlEpisode.advance("x".repeat(129), "parcool:dodge", "parcool:vault"));
    }

    @Test
    void repeatingTheFirstActionDoesNotComplete() {
        assertEquals(
                DistinctControlEpisode.Transition.REPEAT,
                DistinctControlEpisode.advance("episode-1", "parcool:dodge", "parcool:dodge"));
    }

    @Test
    void aDifferentActionCompletesTheEpisode() {
        assertEquals(
                DistinctControlEpisode.Transition.DISTINCT_SECOND,
                DistinctControlEpisode.advance("episode-1", "parcool:dodge", "parcool:vault"));
    }
}
