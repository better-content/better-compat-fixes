package com.bettercontent.bettercompatfixes.api;

import java.util.function.Supplier;

/** Actual vanilla natural selection/admission only, never the surrounding tick or finalization.
 * External spawners can use NATURAL too; that enum alone is not authority to replace their actors.
 */
public final class NaturalSpawnContext {
    public enum Stage { SELECTION, PLACEMENT, POSITION }
    private static final ActorScopes<Stage> SCOPES = new ActorScopes<>();
    private NaturalSpawnContext() { }
    public static boolean active() { return stage() != null; }
    public static Stage stage() { return SCOPES.current(); }
    public static <T> T during(Supplier<T> action) { return during(Stage.POSITION, action); }
    public static <T> T during(Stage stage, Supplier<T> action) { return SCOPES.call(stage, action); }
}
