package com.bettercontent.bettercompatfixes.compat;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

final class DynamicTreesPoissonDiscSerializationTest {
    @Test
    void serializesConcurrentCallbacksForOneProvider() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            final Object provider = new Object();
            final AtomicInteger inside = new AtomicInteger();
            final AtomicInteger maximumInside = new AtomicInteger();
            final CountDownLatch ready = new CountDownLatch(8);
            final CountDownLatch start = new CountDownLatch(1);

            final ExecutorService executor = Executors.newFixedThreadPool(8);
            try {
                for (int index = 0; index < 8; index++) {
                    executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        DynamicTreesPoissonDiscSerialization.run(provider, () -> {
                            final int current = inside.incrementAndGet();
                            maximumInside.accumulateAndGet(current, Math::max);
                            try {
                                TimeUnit.MILLISECONDS.sleep(10);
                            } catch (final InterruptedException exception) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(exception);
                            } finally {
                                inside.decrementAndGet();
                            }
                        });
                        return null;
                    });
                }

                ready.await();
                start.countDown();
                executor.shutdown();
                executor.awaitTermination(3, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }

            assertEquals(1, maximumInside.get());
        });
    }

    @Test
    void returnsValuesFromSerializedCalls() {
        assertEquals(
                "poisson-data",
                DynamicTreesPoissonDiscSerialization.call(new Object(), () -> "poisson-data")
        );
    }
}
