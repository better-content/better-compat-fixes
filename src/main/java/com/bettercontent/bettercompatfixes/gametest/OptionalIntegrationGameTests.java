package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.RealisticBlockPhysicsDefinitions;
import com.bettercontent.bettercompatfixes.compat.VoidWormSpawnRemoval;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.MobSpawnSettingsBuilder;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@PrefixGameTestTemplate(false)
public final class OptionalIntegrationGameTests {
    private OptionalIntegrationGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void spawnRemovalTargetsOnlyTheRequestedEntity(final GameTestHelper helper) {
        final MobSpawnSettings settings = new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.ZOMBIE, 1, 1, 1))
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.SKELETON, 1, 1, 1))
                .build();
        final MobSpawnSettingsBuilder builder = new MobSpawnSettingsBuilder(settings);
        final int removed = VoidWormSpawnRemoval.removeSpawns(
                builder, ResourceLocation.fromNamespaceAndPath("minecraft", "zombie"));

        if (removed != 1 || builder.getSpawner(MobCategory.MONSTER).size() != 1
                || builder.getSpawner(MobCategory.MONSTER).get(0).type != EntityType.SKELETON) {
            helper.fail("Biome spawn removal did not preserve unrelated spawns");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void thirstLootModifierCodecIsRegisteredWhenPresent(final GameTestHelper helper) {
        if (ModList.get().isLoaded("thirst") && !ForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.get().containsKey(
                ResourceLocation.fromNamespaceAndPath("thirst", "add_loot_table"))) {
            helper.fail("Thirst add_loot_table codec was not registered");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void thirstNestedChestLootDoesNotReenterGlobalModifiers(final GameTestHelper helper) {
        if (!ModList.get().isLoaded("thirst")) {
            helper.succeed();
            return;
        }
        final LootTable table = helper.getLevel().getServer().getLootData().getLootTable(
                ResourceLocation.fromNamespaceAndPath("minecraft", "chests/simple_dungeon"));
        final LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(LootContextParamSets.CHEST);
        table.getRandomItems(params);
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void realisticBlockPhysicsTargetsOnlyUsableStates(final GameTestHelper helper) {
        if (ModList.get().isLoaded("rbp") && (
                !RealisticBlockPhysicsDefinitions.isSupportedState(Blocks.STONE.defaultBlockState())
                        || RealisticBlockPhysicsDefinitions.isSupportedState(Blocks.WATER.defaultBlockState())
                        || RealisticBlockPhysicsDefinitions.isSupportedState(Blocks.DANDELION.defaultBlockState()))) {
            helper.fail("Realistic Block Physics compatibility accepted an unsupported block state");
            return;
        }
        helper.succeed();
    }
}
