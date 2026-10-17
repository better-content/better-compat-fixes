package com.bettercontent.bettercompatfixes.client;

import java.util.function.Function;
import java.util.function.Predicate;

/** Keeps a lower aquatic POV clip from replacing a visible attack overlay's combined pose. */
public final class FirstPersonPovPriority {
    private FirstPersonPovPriority() {}

    public static <L> Function<L, Boolean> guard(
            final boolean aquaticMotion,
            final Function<L, Boolean> nativeSelector,
            final Predicate<L> activeAttackWithoutPov,
            final Runnable clearPov
    ) {
        if (!aquaticMotion) return nativeSelector;
        return layer -> {
            if (activeAttackWithoutPov.test(layer)) {
                clearPov.run();
                // Epic Fight visits high layers first: stop before the lower swim/float POV.
                return Boolean.FALSE;
            }
            return nativeSelector.apply(layer);
        };
    }
}
