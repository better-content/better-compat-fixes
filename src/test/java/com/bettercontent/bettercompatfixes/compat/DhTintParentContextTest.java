package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.mixin.distanthorizons.TintGetterOverrideMixin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DhTintParentContextTest {
    private static final class ContextGetter implements DhTintParentContext {
        Object level;
        public void bettercompatfixes$setTintParent(Object level) { this.level = level; }
    }

    @Test void eachUpdateRefreshesItsOwnContextAndNeverReusesAnOldDimension() {
        Object overworld = new Object(), aether = new Object();
        var getter = new ContextGetter();
        DhTintParentContext.refresh(getter, () -> overworld); assertSame(overworld, getter.level);
        DhTintParentContext.refresh(getter, () -> aether); assertSame(aether, getter.level);
        DhTintParentContext.refresh(getter, () -> overworld); assertSame(overworld, getter.level);
        // The level-free path must not query or require any wrapped level.
        DhTintParentContext.refresh(new Object(), () -> { throw new AssertionError("level-free lookup"); });
        var independent = new ContextGetter();
        DhTintParentContext.refresh(independent, () -> aether);
        assertSame(overworld, getter.level);
        assertSame(aether, independent.level);
    }

    @Test void actualParentBridgeRejectsInvalidContextWithoutSuppressingFailures() {
        var getter = new TintGetterOverrideMixin() {};
        assertThrows(NullPointerException.class, () -> DhTintParentContext.refresh(getter, () -> null));
        assertThrows(ClassCastException.class, () -> DhTintParentContext.refresh(getter, Object::new));
        assertThrows(IllegalStateException.class, () -> DhTintParentContext.refresh(getter,
                () -> { throw new IllegalStateException("context failed"); }));
    }

    public static void main(String[] args) {
        var test = new DhTintParentContextTest();
        test.eachUpdateRefreshesItsOwnContextAndNeverReusesAnOldDimension();
        test.actualParentBridgeRejectsInvalidContextWithoutSuppressingFailures();
    }
}
