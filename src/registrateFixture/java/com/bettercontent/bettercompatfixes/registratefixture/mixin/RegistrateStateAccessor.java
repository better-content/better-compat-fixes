package com.bettercontent.bettercompatfixes.registratefixture.mixin;

import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.tterrag.registrate.AbstractRegistrate", remap = false)
public interface RegistrateStateAccessor {
    @Accessor("registerCallbacks") Multimap<?, ?> bc$pending();
    @Accessor("registrations") Table<?, ?, ?> bc$registrations();
}
