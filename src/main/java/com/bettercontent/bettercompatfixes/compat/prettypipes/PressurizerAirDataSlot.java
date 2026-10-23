package com.bettercontent.bettercompatfixes.compat.prettypipes;

import net.minecraft.world.inventory.DataSlot;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Ordinary menu helper, deliberately outside every reserved Mixin namespace. */
public final class PressurizerAirDataSlot extends DataSlot {
    private final IntSupplier getter;
    private final IntConsumer setter;
    public PressurizerAirDataSlot(IntSupplier getter, IntConsumer setter) { this.getter = getter; this.setter = setter; }
    @Override public int get() { return getter.getAsInt(); }
    @Override public void set(int value) { setter.accept(value); }
}
