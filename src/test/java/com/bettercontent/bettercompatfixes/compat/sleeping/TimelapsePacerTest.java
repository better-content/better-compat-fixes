package com.bettercontent.bettercompatfixes.compat.sleeping;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimelapsePacerTest {
    @AfterEach
    void reset() {
        TimelapsePacer.resetForTest();
    }

    @Test
    void eightHundredTpsUsesOnePointTwoFiveMillisecondIntervals() {
        AtomicLong now = new AtomicLong(10_000L);
        AtomicLong parked = new AtomicLong();
        TimelapsePacer.pace(true, 800, now::get, nanos -> {
            parked.addAndGet(nanos);
            now.addAndGet(nanos);
        });
        TimelapsePacer.pace(true, 800, now::get, nanos -> {
            parked.addAndGet(nanos);
            now.addAndGet(nanos);
        });
        assertEquals(2_500_000L, parked.get());
    }

    @Test
    void inactiveTickResetsTheDeadline() {
        AtomicLong parked = new AtomicLong();
        TimelapsePacer.pace(true, 800, () -> 0L, parked::addAndGet);
        TimelapsePacer.pace(false, 800, () -> 0L, parked::addAndGet);
        TimelapsePacer.pace(true, 1000, () -> 0L, parked::addAndGet);
        assertEquals(2_250_000L, parked.get());
    }
}
