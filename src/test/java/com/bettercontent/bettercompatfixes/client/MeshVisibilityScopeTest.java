package com.bettercontent.bettercompatfixes.client;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MeshVisibilityScopeTest {
    private static final class Part {
        boolean hidden;
        Part(boolean hidden) { this.hidden = hidden; }
    }
    private static void draw(List<Part> parts, Runnable mask, Runnable delegate) {
        MeshVisibilityScope.draw(parts, p -> p.hidden, (p, value) -> p.hidden = value, mask, delegate);
    }

    @Test
    void restoresMixedNativeFlagsAndDelegatesExactlyOnce() {
        Part arm = new Part(false), nativeHidden = new Part(true);
        List<Part> parts = List.of(arm, nativeHidden);
        AtomicInteger calls = new AtomicInteger();
        draw(parts, () -> parts.forEach(p -> p.hidden = true), () -> {
            calls.incrementAndGet();
            assertTrue(arm.hidden);
            assertTrue(nativeHidden.hidden);
        });
        assertEquals(1, calls.get());
        assertFalse(arm.hidden);
        assertTrue(nativeHidden.hidden);
    }

    @Test
    void drawExceptionRestoresFlagsAndPropagatesOriginalFailure() {
        Part arm = new Part(false), nativeHidden = new Part(true);
        List<Part> parts = List.of(arm, nativeHidden);
        AtomicInteger calls = new AtomicInteger();
        RuntimeException failure = new RuntimeException("native draw failure");
        assertSame(failure, assertThrows(RuntimeException.class, () -> draw(parts,
                () -> parts.forEach(p -> p.hidden = true), () -> {
                    calls.incrementAndGet();
                    throw failure;
                })));
        assertEquals(1, calls.get());
        assertFalse(arm.hidden);
        assertTrue(nativeHidden.hidden);
    }

    @Test
    void partialMaskExceptionRestoresFlagsWithoutCallingDraw() {
        Part arm = new Part(false), nativeHidden = new Part(true);
        AtomicInteger calls = new AtomicInteger();
        RuntimeException failure = new RuntimeException("mask failure");
        assertSame(failure, assertThrows(RuntimeException.class, () -> draw(List.of(arm, nativeHidden), () -> {
            arm.hidden = true;
            nativeHidden.hidden = false;
            throw failure;
        }, calls::incrementAndGet)));
        assertEquals(0, calls.get());
        assertFalse(arm.hidden);
        assertTrue(nativeHidden.hidden);
    }

    @Test
    void repeatedDrawsNeverCarryFirstPersonMaskIntoNextPass() {
        Part arm = new Part(false), head = new Part(true);
        for (int i = 0; i < 2; i++) {
            draw(List.of(arm, head), () -> arm.hidden = true, () -> assertTrue(arm.hidden));
            assertFalse(arm.hidden);
            assertTrue(head.hidden);
        }
    }
}
