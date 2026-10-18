package com.bettercontent.bettercompatfixes.client;

import java.util.Arrays;

public final class DirectionalDoubleTapTracker {
    private static final int DIRECTION_COUNT = 4;

    private final boolean[] previousDown = new boolean[DIRECTION_COUNT];
    private final long[] firstPressTick = new long[DIRECTION_COUNT];
    private final boolean[] releasedAfterFirstPress = new boolean[DIRECTION_COUNT];
    private long tick;

    public DirectionalDoubleTapTracker() {
        Arrays.fill(firstPressTick, -1L);
    }

    public boolean update(
            final boolean forward,
            final boolean backward,
            final boolean left,
            final boolean right,
            final int windowTicks
    ) {
        final boolean[] down = {forward, backward, left, right};
        tick++;

        for (int direction = 0; direction < DIRECTION_COUNT; direction++) {
            if (firstPressTick[direction] >= 0L && tick - firstPressTick[direction] > windowTicks) {
                disarm(direction);
            }
            if (previousDown[direction] && !down[direction] && firstPressTick[direction] >= 0L) {
                releasedAfterFirstPress[direction] = true;
            }
        }

        boolean triggered = false;
        for (int direction = 0; direction < DIRECTION_COUNT; direction++) {
            final boolean risingEdge = down[direction] && !previousDown[direction];
            if (!risingEdge) {
                continue;
            }
            if (firstPressTick[direction] >= 0L && releasedAfterFirstPress[direction]) {
                triggered = true;
            } else {
                firstPressTick[direction] = tick;
                releasedAfterFirstPress[direction] = false;
            }
        }

        System.arraycopy(down, 0, previousDown, 0, DIRECTION_COUNT);
        if (triggered) {
            clearArmedTaps();
        }
        return triggered;
    }

    public static float axisImpulse(final boolean positive, final boolean negative) {
        return (positive ? 1.0F : 0.0F) - (negative ? 1.0F : 0.0F);
    }

    public void reset() {
        Arrays.fill(previousDown, false);
        clearArmedTaps();
        tick = 0L;
    }

    private void clearArmedTaps() {
        Arrays.fill(firstPressTick, -1L);
        Arrays.fill(releasedAfterFirstPress, false);
    }

    private void disarm(final int direction) {
        firstPressTick[direction] = -1L;
        releasedAfterFirstPress[direction] = false;
    }
}
