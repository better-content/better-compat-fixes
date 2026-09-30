package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class AmbientSurfaceSpawnGameTests {
    private AmbientSurfaceSpawnGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void naturalMonsterIsDeniedAtLeafCoveredSurface(final GameTestHelper helper) {
        final BlockPos testColumn = helper.absolutePos(new BlockPos(2, 1, 2));
        final int surfaceY = helper.getLevel().getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                testColumn.getX(),
                testColumn.getZ());
        final BlockPos surface = new BlockPos(testColumn.getX(), surfaceY, testColumn.getZ());
        helper.getLevel().setBlockAndUpdate(surface.above(4), Blocks.OAK_LEAVES.defaultBlockState());

        final MobSpawnEvent.PositionCheck event = positionCheck(helper, surface, MobSpawnType.NATURAL);
        if (event.getResult() != Event.Result.DENY) {
            final int observedSurfaceY = helper.getLevel().getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    surface.getX(),
                    surface.getZ());
            helper.fail("Expected natural monster spawning beneath leaves to be denied as surface pressure"
                    + " (spawnY=" + surface.getY()
                    + ", surfaceY=" + observedSurfaceY
                    + ", dimension=" + helper.getLevel().dimension().location()
                    + ", enabled=" + BcFixesConfig.mobsBlockNaturalSurfaceHostiles()
                    + ", depth=" + BcFixesConfig.mobsNaturalSurfaceDepth()
                    + ")");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void deepNaturalMonsterAndAuthoredSourcesRemainAvailable(final GameTestHelper helper) {
        final BlockPos testColumn = helper.absolutePos(new BlockPos(2, 1, 2));
        final int surfaceY = helper.getLevel().getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                testColumn.getX(),
                testColumn.getZ());
        final BlockPos surface = new BlockPos(testColumn.getX(), surfaceY, testColumn.getZ());
        final BlockPos deepPosition = surface.below(7);

        final MobSpawnEvent.PositionCheck natural = positionCheck(
                helper, deepPosition, MobSpawnType.CHUNK_GENERATION, EntityType.SPIDER);
        if (natural.getResult() == Event.Result.DENY) {
            helper.fail("Expected natural monsters below the six-block surface band to remain available");
            return;
        }

        final MobSpawnEvent.PositionCheck eventSpawn = positionCheck(helper, surface, MobSpawnType.EVENT);
        if (eventSpawn.getResult() == Event.Result.DENY) {
            helper.fail("Expected authored event spawning at the surface to remain available");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void taggedGrassDeniesAmbientMonstersAtAnyDepth(final GameTestHelper helper) {
        final BlockPos testColumn = helper.absolutePos(new BlockPos(2, 1, 2));
        final int surfaceY = helper.getLevel().getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                testColumn.getX(),
                testColumn.getZ());
        final BlockPos deepPosition = new BlockPos(testColumn.getX(), surfaceY - 12, testColumn.getZ());
        helper.getLevel().setBlockAndUpdate(deepPosition.below(), Blocks.GRASS_BLOCK.defaultBlockState());

        final MobSpawnEvent.PositionCheck natural = positionCheck(helper, deepPosition, MobSpawnType.NATURAL);
        if (natural.getResult() != Event.Result.DENY) {
            helper.fail("Expected tagged grass to deny natural monsters below the heightmap surface band");
            return;
        }

        final MobSpawnEvent.PositionCheck chunkGeneration =
                positionCheck(helper, deepPosition, MobSpawnType.CHUNK_GENERATION);
        if (chunkGeneration.getResult() != Event.Result.DENY) {
            helper.fail("Expected tagged grass to deny chunk-generation monsters below the heightmap surface band");
            return;
        }

        final MobSpawnEvent.PositionCheck eventSpawn = positionCheck(helper, deepPosition, MobSpawnType.EVENT);
        if (eventSpawn.getResult() == Event.Result.DENY) {
            helper.fail("Expected authored event spawning on tagged grass to remain available");
            return;
        }
        helper.succeed();
    }

    private static MobSpawnEvent.PositionCheck positionCheck(
            final GameTestHelper helper,
            final BlockPos position,
            final MobSpawnType spawnType) {
        return positionCheck(helper, position, spawnType, EntityType.ZOMBIE);
    }

    private static <T extends net.minecraft.world.entity.Mob> MobSpawnEvent.PositionCheck positionCheck(
            final GameTestHelper helper,
            final BlockPos position,
            final MobSpawnType spawnType,
            final EntityType<T> entityType) {
        final T mob = entityType.create(helper.getLevel());
        if (mob == null) {
            throw new IllegalStateException("Could not create test mob " + entityType);
        }
        mob.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, 0.0F, 0.0F);
        final MobSpawnEvent.PositionCheck event = new MobSpawnEvent.PositionCheck(
                mob,
                helper.getLevel(),
                spawnType,
                null);
        MinecraftForge.EVENT_BUS.post(event);
        mob.discard();
        return event;
    }
}
