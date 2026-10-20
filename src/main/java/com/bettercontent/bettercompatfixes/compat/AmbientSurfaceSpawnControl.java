package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.api.event.GeologicalAmbientEligibilityEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Keeps ordinary ambient monsters below the Overworld surface pressure band and off grass-covered ground. */
public final class AmbientSurfaceSpawnControl {
    private AmbientSurfaceSpawnControl() {
    }

    @SubscribeEvent
    public static void onPositionCheck(final MobSpawnEvent.PositionCheck event) {
        final boolean overworld = event.getLevel().getLevel().dimension().equals(Level.OVERWORLD);
        final MobCategory category = event.getEntity().getType().getCategory();
        final MobSpawnType spawnType = event.getSpawnType();
        if (shouldDenyVanillaAmbient(
                BcFixesConfig.mobsBlockVanillaZombiesAndSkeletons(),
                overworld,
                ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()).toString(),
                spawnType)) {
            event.setResult(Event.Result.DENY);
            return;
        }

        // Pack-owned replacement for the former In Control Y<=62 natural-only rule.
        // The query waives this one restriction, never another listener's DENY result.
        if (overworld && spawnType == MobSpawnType.NATURAL && event.getY() <= 62
                && event.getEntity().getType().is(Tags.LEGACY_UNDERGROUND_DENIED)
                && !GeologicalAmbientEligibilityEvent.eligible(event.getLevel(),
                    BlockPos.containing(event.getX(), event.getY(), event.getZ()),
                    event.getEntity().getType(), spawnType)) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (!isAmbientSurfaceCandidate(overworld, category, spawnType)) {
            return;
        }

        if (!BcFixesConfig.mobsBlockNaturalSurfaceHostiles()) {
            return;
        }

        final int spawnX = Mth.floor(event.getX());
        final int spawnY = Mth.floor(event.getY());
        final int spawnZ = Mth.floor(event.getZ());
        final int surfaceY = event.getLevel().getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                spawnX,
                spawnZ);
        final boolean deniedSurface = event.getLevel()
                .getBlockState(new BlockPos(spawnX, spawnY - 1, spawnZ))
                .is(Tags.AMBIENT_SPAWN_DENIED_SURFACES);

        if (shouldDeny(
                overworld,
                category,
                spawnType,
                spawnY,
                surfaceY,
                BcFixesConfig.mobsNaturalSurfaceDepth(),
                deniedSurface)) {
            event.setResult(Event.Result.DENY);
        }
    }

    static boolean shouldDeny(
            final boolean overworld,
            final MobCategory category,
            final MobSpawnType spawnType,
            final int spawnY,
            final int surfaceY,
            final int surfaceDepth,
            final boolean deniedSurface) {
        return isAmbientSurfaceCandidate(overworld, category, spawnType)
                && (deniedSurface || spawnY >= surfaceY - surfaceDepth);
    }

    static boolean shouldDenyVanillaAmbient(
            final boolean enabled,
            final boolean overworld,
            final String entityId,
            final MobSpawnType spawnType) {
        return enabled
                && overworld
                && (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION)
                && ("minecraft:zombie".equals(entityId) || "minecraft:skeleton".equals(entityId));
    }

    private static boolean isAmbientSurfaceCandidate(
            final boolean overworld,
            final MobCategory category,
            final MobSpawnType spawnType) {
        return overworld
                && category == MobCategory.MONSTER
                && (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION);
    }

    private static final class Tags {
        private static final TagKey<EntityType<?>> LEGACY_UNDERGROUND_DENIED = TagKey.create(
                Registries.ENTITY_TYPE, new ResourceLocation(BetterContentFixes.MOD_ID,
                        "legacy_underground_ambient_denied"));
        private static final TagKey<Block> AMBIENT_SPAWN_DENIED_SURFACES = BlockTags.create(
                new ResourceLocation(BetterContentFixes.MOD_ID, "ambient_spawn_denied_surfaces"));

        private Tags() {
        }
    }
}
