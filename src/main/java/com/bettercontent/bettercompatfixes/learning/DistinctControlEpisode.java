package com.bettercontent.bettercompatfixes.learning;

import java.util.Objects;

/** Pure transition policy for a two-distinct-control learning episode. */
final class DistinctControlEpisode {
    private DistinctControlEpisode() {
    }

    static Transition advance(final String token, final String firstAction, final String currentAction) {
        Objects.requireNonNull(currentAction, "currentAction");
        if (!validToken(token) || firstAction == null || firstAction.isBlank()) {
            return Transition.FIRST;
        }
        return firstAction.equals(currentAction) ? Transition.REPEAT : Transition.DISTINCT_SECOND;
    }

    private static boolean validToken(final String token) {
        if (token == null || token.isBlank() || token.length() > 128) {
            return false;
        }
        for (int index = 0; index < token.length(); index++) {
            final char character = token.charAt(index);
            if (character < 0x21 || character > 0x7e) {
                return false;
            }
        }
        return true;
    }

    enum Transition {
        FIRST,
        REPEAT,
        DISTINCT_SECOND
    }
}
