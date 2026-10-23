package com.bettercontent.bettercompatfixes.compat.bees;

/** Bounded call-local primitives only; never retains native genes, stacks, housing or worlds. */
public final class DecodedColorScope {
    public static final int MAX_DEPTH = 8;
    public record Sample(boolean known, int rgb) {
        public static final Sample UNKNOWN = new Sample(false, 0);
        public Sample {
            if (rgb < 0 || rgb > 0xffffff || !known && rgb != 0) throw new IllegalArgumentException("Noncanonical RGB24");
        }
    }
    public static final class Frame {
        private final Frame previous;
        private final int depth;
        private int captures, rgb;
        private boolean known;
        private Frame(Frame previous) { this.previous = previous; depth = previous == null ? 1 : previous.depth + 1; }
    }
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Integer> SUPPRESSED = ThreadLocal.withInitial(() -> 0);
    private DecodedColorScope() { }
    public static boolean capturing() { return SUPPRESSED.get() == 0 && CURRENT.get() != null; }
    public static Frame open() {
        Frame previous = CURRENT.get();
        int suppressed = SUPPRESSED.get();
        if (suppressed > 0 || previous != null && previous.depth >= MAX_DEPTH) {
            SUPPRESSED.set(suppressed + 1); return null;
        }
        Frame frame = new Frame(previous); CURRENT.set(frame); return frame;
    }
    public static void capture(boolean known, int rgb) {
        if (!capturing()) return;
        Frame frame = CURRENT.get();
        frame.captures = Math.min(2, frame.captures + 1);
        frame.known = known && rgb >= 0 && rgb <= 0xffffff;
        frame.rgb = frame.known ? rgb : 0;
    }
    /** Always close in finally. Sampling never substitutes a native call's return/exception. */
    public static Sample close(Frame frame) {
        if (frame == null) {
            int suppressed = SUPPRESSED.get();
            if (suppressed <= 1) SUPPRESSED.remove(); else SUPPRESSED.set(suppressed - 1);
            return Sample.UNKNOWN;
        }
        if (CURRENT.get() != frame) { CURRENT.remove(); SUPPRESSED.remove(); return Sample.UNKNOWN; }
        if (frame.previous == null) CURRENT.remove(); else CURRENT.set(frame.previous);
        if (frame.captures != 1 || !frame.known) return Sample.UNKNOWN;
        return new Sample(true, frame.rgb);
    }
}
