package com.bettercontent.bettercompatfixes.compat.emi;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RecipeCombinationsTest {
    @Test void enumeratesAllCombinationsInStableOrder() {
        var rows = new ArrayList<List<String>>();
        assertTrue(RecipeCombinations.visit(List.of(List.of("a", "b"), List.of("x", "y")), 4, rows::add));
        assertEquals(List.of(List.of("a", "x"), List.of("a", "y"), List.of("b", "x"), List.of("b", "y")), rows);
        assertThrows(UnsupportedOperationException.class, () -> rows.get(0).add("mutation"));
    }
    @Test void reportsTruncationInsteadOfSilentlyClaimingCompleteCoverage() {
        var rows = new ArrayList<List<Integer>>();
        assertFalse(RecipeCombinations.visit(List.of(List.of(1, 2), List.of(3, 4)), 3, rows::add));
        assertEquals(3, rows.size());
        assertEquals(List.of(2, 3), rows.get(2));
    }
    @Test void emptyIngredientDoesNotProduceFalseRecipe() {
        var rows = new ArrayList<List<Integer>>();
        assertTrue(RecipeCombinations.visit(List.of(List.of(1), List.<Integer>of()), 2, rows::add));
        assertTrue(rows.isEmpty());
    }
    @Test void noIngredientsHasOneEmptyCombination() {
        var rows = new ArrayList<List<Integer>>();
        assertTrue(RecipeCombinations.<Integer>visit(List.of(), 1, rows::add));
        assertEquals(List.of(List.of()), rows);
    }
    @Test void rejectsNonpositiveBudget() {
        assertThrows(IllegalArgumentException.class, () -> RecipeCombinations.visit(List.of(), 0, ignored -> {}));
    }
    @Test void wideProductsStayBoundedWithoutOverflow() {
        var choices = new ArrayList<List<Integer>>();
        for (int i = 0; i < 100; i++) choices.add(List.of(1, 2));
        var rows = new ArrayList<List<Integer>>();
        assertFalse(RecipeCombinations.visit(choices, 5, rows::add));
        assertEquals(5, rows.size());
        assertEquals(100, rows.get(0).size());
    }
}
