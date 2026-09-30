package com.bettercontent.bettercompatfixes.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class TwilightForestMazeSerializationTest {
    @Test
    void activatesOnlyForTwilightForestAndC2me() {
        assertTrue(TwilightForestMazeSerialization.dependenciesAvailable(
                true, Set.of("twilightforest", "c2me")::contains));
        assertFalse(TwilightForestMazeSerialization.dependenciesAvailable(
                false, Set.of("twilightforest", "c2me")::contains));
        assertFalse(TwilightForestMazeSerialization.dependenciesAvailable(
                true, Set.of("twilightforest")::contains));
        assertFalse(TwilightForestMazeSerialization.dependenciesAvailable(
                true, Set.of("c2me")::contains));
    }

    @Test
    void sharedRandomMonitorSerializesDifferentMazePlacements() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            final Object sharedRandom = new Object();
            final AtomicInteger inside = new AtomicInteger();
            final AtomicInteger maximumInside = new AtomicInteger();
            final CountDownLatch firstEntered = new CountDownLatch(1);
            final CountDownLatch releaseFirst = new CountDownLatch(1);
            final CountDownLatch secondStarted = new CountDownLatch(1);
            final CountDownLatch secondEntered = new CountDownLatch(1);
            final ExecutorService executor = Executors.newFixedThreadPool(2);

            try {
                final Future<?> first = executor.submit(() ->
                        TwilightForestMazeSerialization.runSerialized(sharedRandom, () -> {
                            enter(inside, maximumInside);
                            firstEntered.countDown();
                            await(releaseFirst);
                            inside.decrementAndGet();
                        }));
                assertTrue(firstEntered.await(1, TimeUnit.SECONDS));

                final Future<?> second = executor.submit(() -> {
                    secondStarted.countDown();
                    TwilightForestMazeSerialization.runSerialized(sharedRandom, () -> {
                        enter(inside, maximumInside);
                        secondEntered.countDown();
                        inside.decrementAndGet();
                    });
                });
                assertTrue(secondStarted.await(1, TimeUnit.SECONDS));
                assertFalse(secondEntered.await(100, TimeUnit.MILLISECONDS));

                releaseFirst.countDown();
                first.get(1, TimeUnit.SECONDS);
                second.get(1, TimeUnit.SECONDS);
                assertEquals(1, maximumInside.get());
            } finally {
                releaseFirst.countDown();
                executor.shutdownNow();
            }
        });
    }

    @Test
    void nestedPlacementOnTheSameRandomMonitorIsReentrant() {
        final Object sharedRandom = new Object();
        assertTimeoutPreemptively(Duration.ofSeconds(1), () ->
                TwilightForestMazeSerialization.runSerialized(sharedRandom, () ->
                        TwilightForestMazeSerialization.runSerialized(sharedRandom, () -> { })));
    }

    private static void enter(final AtomicInteger inside, final AtomicInteger maximumInside) {
        final int current = inside.incrementAndGet();
        maximumInside.accumulateAndGet(current, Math::max);
    }

    private static void await(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
