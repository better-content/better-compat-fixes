package com.bettercontent.bettercompatfixes.api;

import java.util.function.Supplier;

/** Stack discipline is independent of ownership: an unowned victim must mask its attacker. */
final class ActorScopes<A> {
    private final ThreadLocal<A> current = new ThreadLocal<>();
    A current() { return current.get(); }
    <T> T call(A actor, Supplier<T> action) {
        A previous = current.get();
        if (actor == null) current.remove(); else current.set(actor);
        try { return action.get(); }
        finally { if (previous == null) current.remove(); else current.set(previous); }
    }
}
