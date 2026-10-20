package com.bettercontent.bettercompatfixes.learning;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ControlEpisodeObservationTest {
    @Test void repeatedNativeDecisionCannotBecomeASecondPublication() {
        var decision = DistinctControlEpisode.advance("existing-token", "private-first-action", "private-first-action");
        assertEquals("unchanged", ControlEpisodeObservation.result(decision));
        assertEquals("repeated_action", ControlEpisodeObservation.reason(decision));
        assertFalse(ControlEpisodeObservation.reason(decision).contains("private"));
    }
    @Test void firstAndDistinctSecondUseDifferentPublishedCausesWithoutRawActions() {
        var first = DistinctControlEpisode.advance("", "", "private-first-action");
        var second = DistinctControlEpisode.advance("existing-token", "private-first-action", "private-second-action");
        assertEquals("published", ControlEpisodeObservation.result(first));
        assertEquals("published", ControlEpisodeObservation.result(second));
        assertEquals("first_accepted", ControlEpisodeObservation.reason(first));
        assertEquals("distinct_second", ControlEpisodeObservation.reason(second));
    }
}
