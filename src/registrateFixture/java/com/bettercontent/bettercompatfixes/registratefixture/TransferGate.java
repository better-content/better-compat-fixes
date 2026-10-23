package com.bettercontent.bettercompatfixes.registratefixture;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** One native-test-owned original-call gap, never a production callback or altered return. */
public final class TransferGate {
    private static volatile TransferGate active;
    private final Object owner;
    public final CountDownLatch reached = new CountDownLatch(1);
    public final CountDownLatch release = new CountDownLatch(1);
    public TransferGate(Object owner) { this.owner = owner; }
    public static void arm(TransferGate gate) { if (active != null) throw new AssertionError("Reentrant fixture gap"); active = gate; }
    public static void clear(TransferGate gate) { gate.release.countDown(); if (active == gate) active = null; }
    public static void afterRemove(Object owner) {
        TransferGate gate = active;
        if (gate == null || gate.owner != owner) return;
        gate.reached.countDown(); await(gate.release);
    }
    public static void await(CountDownLatch latch) {
        try { if (!latch.await(3, TimeUnit.SECONDS)) throw new AssertionError("Native Registrate fixture latch deadline"); }
        catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
    }
}
