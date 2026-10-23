package com.bettercontent.bettercompatfixes.registratefixture.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

@Mixin(targets = "com.tterrag.registrate.AbstractRegistrate$Registration", remap = false)
public interface RegistrationCallbacksAccessor {
    @Accessor("callbacks") List<?> bc$callbacks();
}
