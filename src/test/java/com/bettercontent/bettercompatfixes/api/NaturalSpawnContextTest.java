package com.bettercontent.bettercompatfixes.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NaturalSpawnContextTest {
    @Test void onlyTheScopedNativeCallHasSubstitutionAuthority() throws Exception {
        assertFalse(NaturalSpawnContext.active());
        NaturalSpawnContext.during(() -> {
            assertTrue(NaturalSpawnContext.active());
            NaturalSpawnContext.during(() -> { assertTrue(NaturalSpawnContext.active()); return null; });
            assertTrue(NaturalSpawnContext.active()); return null;
        });
        assertFalse(NaturalSpawnContext.active());
        assertThrows(IllegalStateException.class, () -> NaturalSpawnContext.during(() -> { throw new IllegalStateException(); }));
        assertFalse(NaturalSpawnContext.active());
        var other = new java.util.concurrent.FutureTask<Boolean>(NaturalSpawnContext::active);
        assertFalse(NaturalSpawnContext.during(() -> {
            new Thread(other).start();
            try { return other.get(); } catch (Exception failure) { throw new RuntimeException(failure); }
        }));
        assertFalse(NaturalSpawnContext.active());
    }
}
