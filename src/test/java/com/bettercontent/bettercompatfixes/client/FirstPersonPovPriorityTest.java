package com.bettercontent.bettercompatfixes.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class FirstPersonPovPriorityTest {
    private record Layer(String name, boolean active, boolean attack, boolean pov) {}
    private static final Layer SWIM = new Layer("swim-pov", true, false, true);
    private static final Layer FLOAT = new Layer("float-pov", true, false, true);
    private static final Layer ATTACK = new Layer("attack", true, true, false);

    private static void visit(List<Layer> layers, Function<Layer, Boolean> selector) {
        for (Layer layer : layers) if (!selector.apply(layer)) break;
    }

    private static Function<Layer, Boolean> selector(boolean aquatic, List<String> selected, AtomicInteger clears) {
        return FirstPersonPovPriority.guard(aquatic, layer -> {
            if (layer.active() && layer.pov()) {
                selected.add(layer.name());
                return false;
            }
            return true;
        }, layer -> layer.active() && layer.attack() && !layer.pov(), clears::incrementAndGet);
    }

    @Test void swimAndFloatPovCannotHideAnAttackWithoutItsOwnPov() {
        for (Layer aquatic : List.of(SWIM, FLOAT)) {
            var selected = new ArrayList<String>();
            var clears = new AtomicInteger();
            visit(List.of(ATTACK, aquatic), selector(true, selected, clears));
            assertTrue(selected.isEmpty(), "Use combined pose rather than lower " + aquatic.name());
            assertEquals(1, clears.get(), "Clear the previously selected aquatic POV");
        }
    }

    @Test void dedicatedAttackPovStillWinsWithoutClearing() {
        var selected = new ArrayList<String>();
        var clears = new AtomicInteger();
        visit(List.of(new Layer("attack-pov", true, true, true), SWIM), selector(true, selected, clears));
        assertEquals(List.of("attack-pov"), selected);
        assertEquals(0, clears.get());
    }

    @Test void inactiveAttackAndNonAttackLayersDoNotBlockSwimming() {
        for (Layer overlay : List.of(new Layer("off-attack", false, true, false),
                new Layer("non-attack", true, false, false))) {
            var selected = new ArrayList<String>();
            var clears = new AtomicInteger();
            visit(List.of(overlay, SWIM), selector(true, selected, clears));
            assertEquals(List.of("swim-pov"), selected);
            assertEquals(0, clears.get());
        }
    }

    @Test void higherPriorityPovIsNotDisplacedByALowerAttack() {
        var selected = new ArrayList<String>();
        var clears = new AtomicInteger();
        visit(List.of(new Layer("higher-pov", true, false, true), ATTACK, SWIM),
                selector(true, selected, clears));
        assertEquals(List.of("higher-pov"), selected);
        assertEquals(0, clears.get());
    }

    @Test void swimmingPovResumesOnTheNextTickWithoutAnAttack() {
        var selected = new ArrayList<String>();
        var clears = new AtomicInteger();
        visit(List.of(SWIM), selector(true, selected, clears));
        visit(List.of(ATTACK, SWIM), selector(true, selected, clears));
        visit(List.of(SWIM), selector(true, selected, clears));
        assertEquals(List.of("swim-pov", "swim-pov"), selected);
        assertEquals(1, clears.get());
    }

    @Test void landReturnsTheNativeSelectorWithoutEvenInspectingLayers() {
        Function<Layer, Boolean> original = layer -> true;
        assertSame(original, FirstPersonPovPriority.guard(false, original,
                layer -> { fail("Do not inspect land layers"); return false; },
                () -> fail("Do not clear land POV")));
    }
}
