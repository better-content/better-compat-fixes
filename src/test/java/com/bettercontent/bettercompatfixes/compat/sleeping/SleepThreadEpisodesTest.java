package com.bettercontent.bettercompatfixes.compat.sleeping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class SleepThreadEpisodesTest {
    @Test
    void detectsOnlyTimelapseBoundaries() {
        assertEquals(SleepThreadEpisodes.Transition.START, SleepThreadEpisodes.transition(false, true));
        assertEquals(SleepThreadEpisodes.Transition.FINISH, SleepThreadEpisodes.transition(true, false));
        assertEquals(SleepThreadEpisodes.Transition.NONE, SleepThreadEpisodes.transition(false, false));
        assertEquals(SleepThreadEpisodes.Transition.NONE, SleepThreadEpisodes.transition(true, true));
    }

    @Test
    void reusesOneValidEpisodeToken() {
        assertEquals("persisted", SleepThreadEpisodes.episodeToken(true, "persisted", "generated"));
        assertEquals("generated", SleepThreadEpisodes.episodeToken(false, "persisted", "generated"));
        assertEquals("generated", SleepThreadEpisodes.episodeToken(true, "", "generated"));
    }
}
