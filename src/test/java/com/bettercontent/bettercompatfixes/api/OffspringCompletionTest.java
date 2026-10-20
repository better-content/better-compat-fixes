package com.bettercontent.bettercompatfixes.api;

import com.bettercontent.bettercompatfixes.api.event.MobOffspringAdmissionEvent;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OffspringCompletionTest {
    @Test void actualResultCompletesOnceAndRejectsLateSubscribers() {
        // Entity tokens are deliberately unused: this exercises callback lifecycle without a world.
        MobOffspringAdmissionEvent event = new MobOffspringAdmissionEvent(null, null);
        List<Boolean> results = new ArrayList<>(); event.onCompletion(results::add);
        event.complete(false);
        assertEquals(List.of(false), results);
        assertThrows(IllegalStateException.class, () -> event.complete(true));
        assertThrows(IllegalStateException.class, () -> event.onCompletion(results::add));
    }
    @Test void aBrokenSubscriberCannotSkipAnotherAccountsRollback() {
        MobOffspringAdmissionEvent event = new MobOffspringAdmissionEvent(null, null);
        List<Boolean> results = new ArrayList<>();
        event.onCompletion(inserted -> { throw new IllegalStateException("broken external consumer"); });
        event.onCompletion(results::add);
        assertThrows(IllegalStateException.class, () -> event.complete(false));
        assertEquals(List.of(false), results);
    }
}
