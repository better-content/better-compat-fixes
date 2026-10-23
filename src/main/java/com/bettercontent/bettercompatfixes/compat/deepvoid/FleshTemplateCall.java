package com.bettercontent.bettercompatfixes.compat.deepvoid;

import java.util.function.BiFunction;

/** Single-call forwarding with safe argument identity and original exceptional completion. */
public final class FleshTemplateCall {
    private FleshTemplateCall() {}
    public static <P, R> R call(P position, P pivot, FleshTemplateBounds.Shift shift,
                                BiFunction<P, FleshTemplateBounds.Shift, P> translate,
                                BiFunction<P, P, R> original) {
        if (shift.unchanged()) return original.apply(position, pivot);
        P movedPosition = translate.apply(position, shift);
        P movedPivot = pivot == position ? movedPosition : translate.apply(pivot, shift);
        return original.apply(movedPosition, movedPivot);
    }
}
