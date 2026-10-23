package com.bettercontent.bettercompatfixes.urnfixture;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import java.util.ArrayList;
import java.util.List;

/** Deterministic input to unchanged native algorithms, never a replacement loot/spawn implementation. */
public final class UrnRandomScope implements AutoCloseable {
    private static final ThreadLocal<UrnRandomScope> CURRENT = new ThreadLocal<>();
    private final UrnRandomScope previous;
    public final RandomSource random;
    public int lootContexts;
    public float luck = Float.NaN;
    public final List<Entity> spawned = new ArrayList<>();
    public UrnRandomScope(long seed) {
        previous = CURRENT.get(); random = RandomSource.create(seed); CURRENT.set(this);
    }
    public static RandomSource forLoot(RandomSource nativeRandom, float actualLuck) {
        var scope = CURRENT.get();
        if (scope == null) return nativeRandom;
        scope.lootContexts++; scope.luck = actualLuck; return scope.random;
    }
    public static RandomSource forSpawn(RandomSource nativeRandom) {
        var scope = CURRENT.get(); return scope == null ? nativeRandom : scope.random;
    }
    public static void observeSpawn(Entity entity, boolean inserted) {
        var scope = CURRENT.get();
        if (scope != null && inserted) scope.spawned.add(entity);
    }
    @Override public void close() {
        try { for (Entity entity : spawned) entity.discard(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
}
