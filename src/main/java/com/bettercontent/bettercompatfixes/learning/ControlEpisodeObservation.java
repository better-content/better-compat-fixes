package com.bettercontent.bettercompatfixes.learning;

/** Projection of the existing pure episode decision; publication is logged only after the native post. */
final class ControlEpisodeObservation {
    private ControlEpisodeObservation() {}
    static String result(DistinctControlEpisode.Transition transition) {
        return transition == DistinctControlEpisode.Transition.REPEAT ? "unchanged" : "published";
    }
    static String reason(DistinctControlEpisode.Transition transition) {
        return switch (transition) {
            case FIRST -> "first_accepted";
            case REPEAT -> "repeated_action";
            case DISTINCT_SECOND -> "distinct_second";
        };
    }
}
