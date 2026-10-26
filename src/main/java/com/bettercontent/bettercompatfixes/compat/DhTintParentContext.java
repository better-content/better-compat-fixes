package com.bettercontent.bettercompatfixes.compat;

/** Implemented only by DH's level-dependent tint getter, not its level-free fast path. */
public interface DhTintParentContext {
    void bettercompatfixes$setTintParent(Object wrappedLevel);

    static void refresh(Object getter, java.util.function.Supplier<Object> wrappedLevel) {
        if (getter instanceof DhTintParentContext context) {
            context.bettercompatfixes$setTintParent(wrappedLevel.get());
        }
    }
}
