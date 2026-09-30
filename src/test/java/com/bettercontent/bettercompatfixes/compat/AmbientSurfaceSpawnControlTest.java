package com.bettercontent.bettercompatfixes.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import org.junit.jupiter.api.Test;

final class AmbientSurfaceSpawnControlTest {
    @Test
    void deniesNaturalAndChunkGenerationMonstersThroughInclusiveSurfaceBand() {
        assertTrue(AmbientSurfaceSpawnControl.shouldDeny(
                true, MobCategory.MONSTER, MobSpawnType.NATURAL, 80, 80, 6, false));
        assertTrue(AmbientSurfaceSpawnControl.shouldDeny(
                true, MobCategory.MONSTER, MobSpawnType.CHUNK_GENERATION, 74, 80, 6, false));
        assertFalse(AmbientSurfaceSpawnControl.shouldDeny(
                true, MobCategory.MONSTER, MobSpawnType.NATURAL, 73, 80, 6, false));
    }

    @Test
    void deniesAmbientMonstersOnTaggedGroundRegardlessOfDepth() {
        assertTrue(AmbientSurfaceSpawnControl.shouldDeny(
                true, MobCategory.MONSTER, MobSpawnType.NATURAL, 20, 80, 6, true));
        assertTrue(AmbientSurfaceSpawnControl.shouldDeny(
                true, MobCategory.MONSTER, MobSpawnType.CHUNK_GENERATION, 20, 80, 6, true));
        assertFalse(AmbientSurfaceSpawnControl.shouldDeny(
                true, MobCategory.MONSTER, MobSpawnType.EVENT, 20, 80, 6, true));
    }

    @Test
    void leavesOtherDimensionsCategoriesAndSpawnSourcesUntouched() {
        assertFalse(AmbientSurfaceSpawnControl.shouldDeny(
                false, MobCategory.MONSTER, MobSpawnType.NATURAL, 80, 80, 6, true));
        assertFalse(AmbientSurfaceSpawnControl.shouldDeny(
                true, MobCategory.CREATURE, MobSpawnType.NATURAL, 80, 80, 6, true));
        assertFalse(AmbientSurfaceSpawnControl.shouldDeny(
                true, MobCategory.MONSTER, MobSpawnType.SPAWNER, 80, 80, 6, true));
        assertFalse(AmbientSurfaceSpawnControl.shouldDeny(
                true, MobCategory.MONSTER, MobSpawnType.STRUCTURE, 80, 80, 6, true));
        assertFalse(AmbientSurfaceSpawnControl.shouldDeny(
                true, MobCategory.MONSTER, MobSpawnType.EVENT, 80, 80, 6, true));
    }

    @Test
    void blocksOnlyAmbientBaseZombiesAndSkeletons() {
        assertTrue(AmbientSurfaceSpawnControl.shouldDenyVanillaAmbient(
                true, true, "minecraft:zombie", MobSpawnType.NATURAL));
        assertTrue(AmbientSurfaceSpawnControl.shouldDenyVanillaAmbient(
                true, true, "minecraft:skeleton", MobSpawnType.CHUNK_GENERATION));
        assertFalse(AmbientSurfaceSpawnControl.shouldDenyVanillaAmbient(
                true, true, "minecraft:zombie", MobSpawnType.SPAWNER));
        assertFalse(AmbientSurfaceSpawnControl.shouldDenyVanillaAmbient(
                true, true, "minecraft:husk", MobSpawnType.NATURAL));
        assertFalse(AmbientSurfaceSpawnControl.shouldDenyVanillaAmbient(
                true, false, "minecraft:skeleton", MobSpawnType.NATURAL));
        assertFalse(AmbientSurfaceSpawnControl.shouldDenyVanillaAmbient(
                false, true, "minecraft:zombie", MobSpawnType.NATURAL));
    }
}
