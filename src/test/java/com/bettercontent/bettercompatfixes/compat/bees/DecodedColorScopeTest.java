package com.bettercontent.bettercompatfixes.compat.bees;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DecodedColorScopeTest {
    @Test void actualNativeRgbValuesRemainUnmapped() {
        for (int rgb : new int[]{0x03fce8, 0xfc030f, 0, 0xffffff}) {
            var frame = DecodedColorScope.open(); DecodedColorScope.capture(true, rgb);
            assertEquals(new DecodedColorScope.Sample(true, rgb), DecodedColorScope.close(frame));
            assertFalse(DecodedColorScope.capturing());
        }
    }
    @Test void zeroDuplicateAndInvalidCapturesClearInsteadOfGuessing() {
        var absent = DecodedColorScope.open(); assertFalse(DecodedColorScope.close(absent).known());
        var duplicate = DecodedColorScope.open();
        DecodedColorScope.capture(true, 0xfc030f); DecodedColorScope.capture(true, 0x03fce8);
        assertFalse(DecodedColorScope.close(duplicate).known());
        for (int bad : new int[]{-1, 0x1000000}) {
            var frame = DecodedColorScope.open(); DecodedColorScope.capture(true, bad);
            assertEquals(DecodedColorScope.Sample.UNKNOWN, DecodedColorScope.close(frame));
        }
    }
    @Test void nestedSamplesDoNotCrossOwnersOrKeepNativeReferences() {
        var outer = DecodedColorScope.open(); DecodedColorScope.capture(true, 0x03fce8);
        var inner = DecodedColorScope.open(); DecodedColorScope.capture(true, 0xfc030f);
        assertEquals(0xfc030f, DecodedColorScope.close(inner).rgb());
        assertEquals(0x03fce8, DecodedColorScope.close(outer).rgb());
        assertFalse(DecodedColorScope.capturing());
    }
    @Test void finallyCleanupPreservesOriginalException() {
        var failure = new IllegalStateException("synthetic");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> {
            var frame = DecodedColorScope.open();
            try { DecodedColorScope.capture(true, 0x03fce8); throw failure; }
            finally { assertTrue(DecodedColorScope.close(frame).known()); }
        }));
        assertFalse(DecodedColorScope.capturing());
    }
    @Test void overflowDoesNotAttributeInnerDataToOuter() {
        DecodedColorScope.Frame[] frames = new DecodedColorScope.Frame[DecodedColorScope.MAX_DEPTH + 2];
        for (int i = 0; i < frames.length; i++) frames[i] = DecodedColorScope.open();
        DecodedColorScope.capture(true, 0xfc030f);
        for (int i = frames.length - 1; i >= 0; i--) assertFalse(DecodedColorScope.close(frames[i]).known());
        assertFalse(DecodedColorScope.capturing());
    }
    @Test void invalidSequenceAndDoubleCloseFailUnknown() {
        var outer = DecodedColorScope.open(); var inner = DecodedColorScope.open();
        assertFalse(DecodedColorScope.close(outer).known());
        assertFalse(DecodedColorScope.close(inner).known());
        assertFalse(DecodedColorScope.close(inner).known());
        assertFalse(DecodedColorScope.capturing());
    }
    @Test void outOfScopeAndUnknownCannotReusePriorColor() {
        DecodedColorScope.capture(true, 0xfc030f);
        var frame = DecodedColorScope.open(); DecodedColorScope.capture(false, 0x03fce8);
        assertEquals(DecodedColorScope.Sample.UNKNOWN, DecodedColorScope.close(frame));
        var next = DecodedColorScope.open(); assertFalse(DecodedColorScope.close(next).known());
    }
    @Test void immutableProtocolSampleRejectsNoncanonicalPayload() {
        assertThrows(IllegalArgumentException.class, () -> new DecodedColorScope.Sample(false, 7));
        assertThrows(IllegalArgumentException.class, () -> new DecodedColorScope.Sample(true, -1));
        assertThrows(IllegalArgumentException.class, () -> new DecodedColorScope.Sample(true, 0x1000000));
    }
}
