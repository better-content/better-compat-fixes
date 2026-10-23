package com.bettercontent.bettercompatfixes.nativefixture;

import com.bettercontent.bettercompatfixes.nativewitness.RootySoilWitness;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Direct actual package-local NONSHIPPING callbacks: no reflection, transformed classes or engine. */
class RootyWitnessForwardingTest {
    @Test void defaultOffPublicCallForwardsExactlyOnceAndNativeFalse() {
        assertFalse(RootySoilWitness.enabled());
        AtomicInteger calls = new AtomicInteger();
        Operation<Boolean> original = args -> {
            calls.incrementAndGet(); assertArrayEquals(new Object[]{null, null, 77}, args); return false;
        };
        assertFalse(new RootySpeciesWitnessMixin() {}.betterCompat$rootyCall(null, null, 77, original));
        assertEquals(1, calls.get());
    }
    @Test void defaultOffSetBlockKeepsOriginalArgumentsFlagsAndFalse() {
        AtomicInteger calls = new AtomicInteger();
        Operation<Boolean> original = args -> {
            calls.incrementAndGet(); assertArrayEquals(new Object[]{null, null, null, 991}, args); return false;
        };
        assertFalse(new RootySpeciesWitnessMixin() {}.betterCompat$setBlock(null, null, null, 991, original));
        assertEquals(1, calls.get());
    }
    @Test void publicNativeExceptionIdentityAndSingleForwardPreserved() {
        for (Throwable originalFailure : new Throwable[]{new IllegalStateException("synthetic"), new AssertionError("synthetic")}) {
            AtomicInteger calls = new AtomicInteger();
            Operation<Boolean> original = args -> {
                calls.incrementAndGet();
                if (originalFailure instanceof RuntimeException runtime) throw runtime;
                throw (Error) originalFailure;
            };
            Throwable actual = assertThrows(Throwable.class, () -> new RootySpeciesWitnessMixin() {}
                    .betterCompat$rootyCall(null, null, 7, original));
            assertSame(originalFailure, actual); assertEquals(1, calls.get());
        }
    }
    @Test void setBlockNativeExceptionIdentityAndSingleForwardPreserved() {
        AssertionError originalFailure = new AssertionError("synthetic"); AtomicInteger calls = new AtomicInteger();
        Operation<Boolean> original = args -> { calls.incrementAndGet(); throw originalFailure; };
        Throwable actual = assertThrows(Throwable.class, () -> new RootySpeciesWitnessMixin() {}
                .betterCompat$setBlock(null, null, null, -31, original));
        assertSame(originalFailure, actual); assertEquals(1, calls.get());
    }
    @Test void actualJoCodeWrapperPreservesReturnAndExceptionWithoutContextQueriesWhenOff() {
        AtomicInteger calls = new AtomicInteger();
        Operation<Boolean> original = args -> {
            calls.incrementAndGet(); assertArrayEquals(new Object[]{null, null, null, -12}, args); return true;
        };
        assertTrue(new RootyJoCodeWitnessMixin() {}.betterCompat$generation(null, null, null, -12, original, null));
        IllegalStateException failure = new IllegalStateException("synthetic");
        Operation<Boolean> throwing = args -> { calls.incrementAndGet(); throw failure; };
        assertSame(failure, assertThrows(Throwable.class, () -> new RootyJoCodeWitnessMixin() {}
                .betterCompat$generation(null, null, null, -12, throwing, null)));
        assertEquals(2, calls.get());
    }
}
