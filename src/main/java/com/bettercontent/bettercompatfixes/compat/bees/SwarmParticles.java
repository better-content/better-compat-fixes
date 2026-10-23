package com.bettercontent.bettercompatfixes.compat.bees;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SwarmParticles {
    public static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, BetterContentFixes.MOD_ID);
    public static final RegistryObject<SimpleParticleType> BEE = TYPES.register("colony_bee", () -> new SimpleParticleType(false));
    public static final RegistryObject<ParticleType<TintedBeeOptions>> TINTED_BEE = TYPES.register("colony_bee_tinted_v1", () ->
            new ParticleType<TintedBeeOptions>(false, TintedBeeOptions.DESERIALIZER) {
                @Override public com.mojang.serialization.Codec<TintedBeeOptions> codec() { return TintedBeeOptions.CODEC; }
            });
    private SwarmParticles() {}
}
