package com.bettercontent.bettercompatfixes.client;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/** A draw-local mask that restores every captured flag, including native-hidden parts. */
public final class MeshVisibilityScope {
    private MeshVisibilityScope() {}

    public static <T> void draw(
            Collection<T> parts,
            Predicate<T> isHidden,
            BiConsumer<T, Boolean> setHidden,
            Runnable applyMask,
            Runnable originalDraw
    ) {
        List<T> snapshot = List.copyOf(parts);
        boolean[] hidden = new boolean[snapshot.size()];
        for (int i = 0; i < snapshot.size(); i++) hidden[i] = isHidden.test(snapshot.get(i));
        try {
            applyMask.run();
            originalDraw.run();
        } finally {
            for (int i = 0; i < snapshot.size(); i++) setHidden.accept(snapshot.get(i), hidden[i]);
        }
    }
}
