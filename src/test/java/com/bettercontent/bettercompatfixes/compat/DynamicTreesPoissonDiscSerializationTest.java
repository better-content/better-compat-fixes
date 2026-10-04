package com.bettercontent.bettercompatfixes.compat;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DynamicTreesPoissonDiscSerializationTest {
    @Test
    void serializesConcurrentCallbacksForOneProvider() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            final Object provider = new Object();
            final AtomicInteger inside = new AtomicInteger();
            final AtomicInteger maximumInside = new AtomicInteger();
            final CountDownLatch firstEntered = new CountDownLatch(1);
            final CountDownLatch releaseFirst = new CountDownLatch(1);
            final CountDownLatch secondStarted = new CountDownLatch(1);
            final CountDownLatch secondEntered = new CountDownLatch(1);
            final AtomicReference<Thread> secondThread = new AtomicReference<>();

            final ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                final Future<?> first = executor.submit(() -> DynamicTreesPoissonDiscSerialization.run(provider, () -> {
                    enter(inside, maximumInside);
                    firstEntered.countDown();
                    await(releaseFirst);
                    inside.decrementAndGet();
                }));
                assertTrue(firstEntered.await(5, TimeUnit.SECONDS));

                final Future<?> second = executor.submit(() -> {
                    secondThread.set(Thread.currentThread());
                    secondStarted.countDown();
                    DynamicTreesPoissonDiscSerialization.run(provider, () -> {
                        enter(inside, maximumInside);
                        secondEntered.countDown();
                        inside.decrementAndGet();
                    });
                });
                assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
                final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (System.nanoTime() < deadline && secondThread.get().getState() != Thread.State.BLOCKED
                        && secondEntered.getCount() != 0) {
                    Thread.onSpinWait();
                }
                assertFalse(secondEntered.getCount() == 0, "second callback entered while first callback held the provider");
                assertTrue(secondThread.get() != null && secondThread.get().getState() == Thread.State.BLOCKED,
                        "second callback never contended for the provider monitor");
                releaseFirst.countDown();
                first.get(5, TimeUnit.SECONDS);
                second.get(5, TimeUnit.SECONDS);
                assertEquals(0, secondEntered.getCount());
                assertEquals(1, maximumInside.get());
            } finally {
                releaseFirst.countDown();
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            }
        });
    }

    @Test
    void returnsValuesFromSerializedCalls() {
        assertEquals(
                "poisson-data",
                DynamicTreesPoissonDiscSerialization.call(new Object(), () -> "poisson-data")
        );
    }

    private static void enter(final AtomicInteger inside, final AtomicInteger maximumInside) {
        maximumInside.accumulateAndGet(inside.incrementAndGet(), Math::max);
    }

    private static void await(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (final InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(failure);
        }
    }
}
