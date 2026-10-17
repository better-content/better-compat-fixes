package com.bettercontent.bettercompatfixes.compat.epicfight;

import java.util.List;

/** Epic Fight motion lists end with dash and air attacks, outside the ordinary combo. */
public final class StyleComboPolicy {
    private StyleComboPolicy() {}

    /** Null delegates unrelated actions to the held capability's original handler. */
    public static Integer counter(List<?> motions, Object animation, int next) {
        if (motions.size() < 3 || animation == null || !motions.contains(animation)) return null;
        int size = motions.size();
        // Test special slots first: some native forms reuse a clip in multiple slots.
        if (animation.equals(motions.get(size - 1)) || animation.equals(motions.get(size - 2))) return 0;
        return next;
    }
}
