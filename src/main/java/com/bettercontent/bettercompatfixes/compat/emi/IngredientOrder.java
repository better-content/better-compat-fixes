package com.bettercontent.bettercompatfixes.compat.emi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Stable partition: never sorts or mutates a recipe-manager-owned list. */
public final class IngredientOrder {
    private IngredientOrder() {}

    public static <T> List<T> availableFirst(List<T> source, Predicate<T> available) {
        List<T> yes = new ArrayList<>();
        List<T> no = new ArrayList<>();
        for (T value : source) {
            (available.test(value) ? yes : no).add(value);
        }
        yes.addAll(no);
        return List.copyOf(yes);
    }
}
