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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class LostCitiesC2meDhSerializationTest {
    @BeforeEach
    void serverStartsInRunningState() {
        LostCitiesC2meDhSerialization.beginServerStarting();
    }

    @AfterEach
    void resetServerLifecycleState() {
        LostCitiesC2meDhSerialization.beginServerStarting();
    }

    @Test
    void activatesForLostCitiesAndC2meWithoutDistantHorizons() {
        final Set<String> loadedMods = Set.of("lostcities", "c2me");

        assertTrue(LostCitiesC2meDhSerialization.dependenciesAvailable(true, loadedMods::contains));
        assertFalse(LostCitiesC2meDhSerialization.dependenciesAvailable(false, loadedMods::contains));
        assertFalse(LostCitiesC2meDhSerialization.dependenciesAvailable(
                true, Set.of("lostcities")::contains));
        assertFalse(LostCitiesC2meDhSerialization.dependenciesAvailable(
                true, Set.of("c2me")::contains));
    }

    @Test
    void outerDecorationAndInnerFeatureCallbacksShareOneLock() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            final AtomicInteger inside = new AtomicInteger();
            final AtomicInteger maximumInside = new AtomicInteger();
            final CountDownLatch outerEntered = new CountDownLatch(1);
            final CountDownLatch releaseOuter = new CountDownLatch(1);
            final CountDownLatch featureStarted = new CountDownLatch(1);
            final CountDownLatch featureEntered = new CountDownLatch(1);
            final ExecutorService executor = Executors.newFixedThreadPool(2);

            try {
                final Future<?> decoration = executor.submit(() ->
                        LostCitiesC2meDhSerialization.runSerialized(() -> {
                            enter(inside, maximumInside);
                            outerEntered.countDown();
                            await(releaseOuter);
                            inside.decrementAndGet();
                        }));
                assertTrue(outerEntered.await(1, TimeUnit.SECONDS));

                final Future<Boolean> feature = executor.submit(() -> {
                    featureStarted.countDown();
                    return LostCitiesC2meDhSerialization.callSerialized(() -> {
                        enter(inside, maximumInside);
                        featureEntered.countDown();
                        inside.decrementAndGet();
                        return true;
                    });
                });
                assertTrue(featureStarted.await(1, TimeUnit.SECONDS));
                assertFalse(featureEntered.await(100, TimeUnit.MILLISECONDS));

                releaseOuter.countDown();
                decoration.get(1, TimeUnit.SECONDS);
                assertTrue(feature.get(1, TimeUnit.SECONDS));
                assertEquals(1, maximumInside.get());
            } finally {
                releaseOuter.countDown();
                executor.shutdownNow();
            }
        });
    }

    @Test
    void nestedFeatureCallbackIsReentrant() {
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertEquals(
                "generated",
                LostCitiesC2meDhSerialization.callSerialized(() ->
                        LostCitiesC2meDhSerialization.callSerialized(() -> "generated"))));
    }

    @Test
    void stoppingSkipsLateLostCityGenerationOnly() {
        assertFalse(LostCitiesC2meDhSerialization.shouldSkipGeneration(true));
        assertFalse(LostCitiesC2meDhSerialization.shouldSkipGeneration(false));

        LostCitiesC2meDhSerialization.beginServerStopping();

        assertTrue(LostCitiesC2meDhSerialization.shouldSkipGeneration(true));
        assertFalse(LostCitiesC2meDhSerialization.shouldSkipGeneration(false));
    }

    @Test
    void shutdownWaitsForActiveLostCityGenerationBeforeClearingCaches() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            final CountDownLatch generationEntered = new CountDownLatch(1);
            final CountDownLatch releaseGeneration = new CountDownLatch(1);
            final CountDownLatch shutdownStarted = new CountDownLatch(1);
            final ExecutorService executor = Executors.newFixedThreadPool(2);

            try {
                final Future<?> generation = executor.submit(() ->
                        LostCitiesC2meDhSerialization.runSerialized(() -> {
                            generationEntered.countDown();
                            await(releaseGeneration);
                        }));
                assertTrue(generationEntered.await(1, TimeUnit.SECONDS));

                final Future<?> shutdown = executor.submit(() -> {
                    shutdownStarted.countDown();
                    LostCitiesC2meDhSerialization.beginServerStopping();
                });
                assertTrue(shutdownStarted.await(1, TimeUnit.SECONDS));
                assertFalse(shutdown.isDone());

                releaseGeneration.countDown();
                generation.get(1, TimeUnit.SECONDS);
                shutdown.get(1, TimeUnit.SECONDS);
                assertTrue(LostCitiesC2meDhSerialization.shouldSkipGeneration(true));
            } finally {
                releaseGeneration.countDown();
                executor.shutdownNow();
            }
        });
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
