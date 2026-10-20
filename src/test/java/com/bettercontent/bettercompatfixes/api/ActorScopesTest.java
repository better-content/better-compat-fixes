package com.bettercontent.bettercompatfixes.api;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ActorScopesTest {
    @Test void unownedVictimMasksOwnedAttackerAndNestedProducerRestoresBoth() {
        ActorScopes<String> scopes = new ActorScopes<>();
        scopes.call("owned_attacker", () -> {
            assertEquals("owned_attacker", scopes.current());
            scopes.call("unowned_victim", () -> {
                assertEquals("unowned_victim", scopes.current());
                scopes.call("actual_corpse_producer", () -> {
                    assertEquals("actual_corpse_producer", scopes.current()); return null;
                });
                assertEquals("unowned_victim", scopes.current()); return null;
            });
            assertEquals("owned_attacker", scopes.current()); return null;
        });
        assertNull(scopes.current());
    }
    @Test void finallyRestoresContextThroughExceptionAndNullScope() {
        ActorScopes<String> scopes = new ActorScopes<>();
        scopes.call("outer", () -> {
            assertThrows(IllegalStateException.class, () -> scopes.call("inner", () -> { throw new IllegalStateException(); }));
            assertEquals("outer", scopes.current());
            scopes.call(null, () -> { assertNull(scopes.current()); return null; });
            assertEquals("outer", scopes.current()); return null;
        });
        assertNull(scopes.current());
    }
    @Test void independentThreadsCannotBorrowCausality() throws InterruptedException {
        ActorScopes<String> scopes = new ActorScopes<>();
        AtomicReference<String> observed = new AtomicReference<>("not_started");
        Thread other = new Thread(() -> observed.set(scopes.current()));
        scopes.call("owned_server_actor", () -> {
            other.start();
            try { other.join(); } catch (InterruptedException e) { throw new AssertionError(e); }
            return null;
        });
        assertNull(observed.get());
    }
}
