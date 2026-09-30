package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.MobSpawnSettingsBuilder;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VoidWormSpawnRemoval implements BiomeModifier {
    public static final DeferredRegister<Codec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, BetterContentFixes.MOD_ID);
    public static final RegistryObject<Codec<VoidWormSpawnRemoval>> CODEC = SERIALIZERS.register(
            "void_worm_spawn_removal",
            () -> Codec.unit(VoidWormSpawnRemoval::new)
    );
    private static final ResourceLocation VOID_WORM = new ResourceLocation("alexsmobs", "void_worm");

    @Override
    public void modify(
            final Holder<Biome> biome,
            final Phase phase,
            final ModifiableBiomeInfo.BiomeInfo.Builder builder
    ) {
        if (phase == Phase.AFTER_EVERYTHING && ModList.get().isLoaded("alexsmobs")) {
            removeSpawns(builder.getMobSpawnSettings(), VOID_WORM);
        }
    }

    public static int removeSpawns(final MobSpawnSettingsBuilder spawns, final ResourceLocation entityId) {
        int removed = 0;
        for (MobCategory category : MobCategory.values()) {
            final int before = spawns.getSpawner(category).size();
            spawns.getSpawner(category).removeIf(entry -> entityId.equals(ForgeRegistries.ENTITY_TYPES.getKey(entry.type)));
            removed += before - spawns.getSpawner(category).size();
        }
        return removed;
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC.get();
    }
}
