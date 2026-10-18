package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.DeepVoidFallBoundary;
import com.mojang.authlib.GameProfile;
import net.mcreator.thedeepvoid.configuration.DeepVoidConfigConfiguration;
import net.mcreator.thedeepvoid.network.TheDeepVoidModVariables;
import net.mcreator.thedeepvoid.procedures.IfPlayerFallsInVoidProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Execute the transformed native decision, observing its real capability before ServerPlayer teleport. */
@PrefixGameTestTemplate(false)
public final class DeepVoidFallBoundaryGameTests {
    private DeepVoidFallBoundaryGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void nativeFallRoutesUseActualBottomAndPreserveSwitches(final GameTestHelper helper) {
        // ConfigValue.set on Forge's autosaving file config races its reload watcher:
        // a previous switch write can reload between a later set and the native read.
        // Keep real ConfigValues/procedure semantics, but bind this synchronous test
        // to an in-memory copy; never write its temporary switches to the fixture file.
        final var spec = DeepVoidConfigConfiguration.SPEC;
        final var originalConfig = net.minecraftforge.fml.config.ConfigTracker.INSTANCE.configSets().values()
                .stream().flatMap(java.util.Collection::stream)
                .filter(config -> config.getSpec() == spec).findFirst().orElseThrow().getConfigData();
        final var fixtureConfig = copyConfig(originalConfig);
        spec.setConfig(fixtureConfig);
        final double oldHeight = DeepVoidConfigConfiguration.TELEPORTHEIGHT.get();
        final boolean oldVoid = DeepVoidConfigConfiguration.PLAYERSTELEPORTINVOID.get();
        final boolean oldOther = DeepVoidConfigConfiguration.ALLOWOTHERWORLDTP.get();
        final boolean oldEnd = DeepVoidConfigConfiguration.PLAYERSTELEPORTINEND.get();
        try {
            // Deliberately unsafe absolute config proves the native expression is really replaced.
            DeepVoidConfigConfiguration.TELEPORTHEIGHT.set(128.0D);
            DeepVoidConfigConfiguration.PLAYERSTELEPORTINEND.set(false);
            for (boolean anyDimension : new boolean[] {false, true}) {
                DeepVoidConfigConfiguration.ALLOWOTHERWORLDTP.set(anyDimension);
                DeepVoidConfigConfiguration.PLAYERSTELEPORTINVOID.set(true);
                final ServerLevel level = helper.getLevel();
                final double threshold = DeepVoidFallBoundary.threshold(level.getMinBuildHeight());
                assertDecision(helper, level, level.getMinBuildHeight(), false);
                assertDecision(helper, level, threshold + 0.001D, false);
                assertDecision(helper, level, threshold, false); // Native normal/any routes are strict <.
                assertDecision(helper, level, threshold - 0.001D, true);
                DeepVoidConfigConfiguration.PLAYERSTELEPORTINVOID.set(false);
                assertDecision(helper, level, threshold - 0.001D, false);
            }
            final ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
            helper.assertTrue(end != null, "Required native End fixture is missing");
            helper.assertTrue(end.getMinBuildHeight() == 0, "End fixture must exercise a non-vanilla-Overworld floor");
            DeepVoidConfigConfiguration.ALLOWOTHERWORLDTP.set(false);
            DeepVoidConfigConfiguration.PLAYERSTELEPORTINVOID.set(false);
            DeepVoidConfigConfiguration.PLAYERSTELEPORTINEND.set(true);
            final double endThreshold = DeepVoidFallBoundary.threshold(end.getMinBuildHeight());
            assertDecision(helper, end, 0.0D, false);
            assertDecision(helper, end, endThreshold + 0.001D, false);
            assertDecision(helper, end, endThreshold, true); // Preserve native End's inclusive <=.
            DeepVoidConfigConfiguration.PLAYERSTELEPORTINEND.set(false);
            assertDecision(helper, end, endThreshold - 0.001D, false);
            // End is otherwise ineligible for the normal void route: distinguish the any-world guard.
            DeepVoidConfigConfiguration.PLAYERSTELEPORTINVOID.set(true);
            DeepVoidConfigConfiguration.ALLOWOTHERWORLDTP.set(false);
            assertDecision(helper, end, endThreshold - 0.001D, false);
            DeepVoidConfigConfiguration.ALLOWOTHERWORLDTP.set(true);
            assertDecision(helper, end, endThreshold - 0.001D, true);
            helper.succeed();
        } finally {
            DeepVoidConfigConfiguration.TELEPORTHEIGHT.set(oldHeight);
            DeepVoidConfigConfiguration.PLAYERSTELEPORTINVOID.set(oldVoid);
            DeepVoidConfigConfiguration.ALLOWOTHERWORLDTP.set(oldOther);
            DeepVoidConfigConfiguration.PLAYERSTELEPORTINEND.set(oldEnd);
            spec.setConfig(originalConfig);
        }
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "deep_void_placement", timeoutTicks = 200)
    public static void nativeHoleTemplatesAndDelayedOpeningsUseExtendedBottom(final GameTestHelper helper) {
        final var level = helper.getLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                new net.minecraft.resources.ResourceLocation(BetterContentFixes.MOD_ID, "deep_void_placement_test")));
        helper.assertTrue(level != null && level.getMinBuildHeight() == -128, "Missing extended-bottom fixture");
        final var spec = DeepVoidConfigConfiguration.SPEC;
        final var originalConfig = net.minecraftforge.fml.config.ConfigTracker.INSTANCE.configSets().values()
                .stream().flatMap(java.util.Collection::stream)
                .filter(config -> config.getSpec() == spec).findFirst().orElseThrow().getConfigData();
        final var variables = TheDeepVoidModVariables.MapVariables.get(level);
        final boolean originalPlaced = variables.CultTemplePlaced;
        final boolean originalRule = level.getGameRules().getBoolean(
                net.mcreator.thedeepvoid.init.TheDeepVoidModGameRules.SPAWN_HOLE_STRUCTURE);
        spec.setConfig(copyConfig(originalConfig));
        final Runnable restore = () -> {
            spec.setConfig(originalConfig);
            variables.CultTemplePlaced = originalPlaced;
            level.getGameRules().getRule(net.mcreator.thedeepvoid.init.TheDeepVoidModGameRules.SPAWN_HOLE_STRUCTURE)
                    .set(originalRule, level.getServer());
        };
        try {
            // Deliberately impossible absolute height proves actual native placement is redirected.
            DeepVoidConfigConfiguration.HOLESTRUCTUREY.set(12345.0D);
            for (boolean old : new boolean[] {true, false}) {
                DeepVoidConfigConfiguration.OLDHOLESTRUCTURE.set(old);
                final int offset = old ? -8 : -12;
                final BlockPos anchor = new BlockPos(offset, -128, offset);
                final var template = level.getStructureManager().getOrCreate(new net.minecraft.resources.ResourceLocation(
                        "the_deep_void", old ? "void_cult_temple" : "cult_temple_noair3"));
                final var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
                final var sample = net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream()
                        .filter(block -> block != net.minecraft.world.level.block.Blocks.AIR
                                && block != net.minecraft.world.level.block.Blocks.STRUCTURE_VOID)
                        .flatMap(block -> template.filterBlocks(anchor, settings, block).stream())
                        .filter(info -> !info.pos().equals(anchor.above()))
                        .findFirst().orElseThrow();
                level.setBlock(sample.pos(), net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState(), 3);
                variables.CultTemplePlaced = false;
                DeepVoidConfigConfiguration.PLACECULTTEMPLE.set(false);
                net.mcreator.thedeepvoid.procedures.CultTemplePlacementProcedure.execute(level);
                helper.assertTrue(!variables.CultTemplePlaced && level.getBlockState(sample.pos()).is(
                        net.minecraft.world.level.block.Blocks.BARRIER), "Disabled placement must remain disabled");
                DeepVoidConfigConfiguration.PLACECULTTEMPLE.set(true);
                level.getGameRules().getRule(net.mcreator.thedeepvoid.init.TheDeepVoidModGameRules.SPAWN_HOLE_STRUCTURE)
                        .set(false, level.getServer());
                net.mcreator.thedeepvoid.procedures.CultTemplePlacementProcedure.execute(level);
                helper.assertTrue(!variables.CultTemplePlaced, "Disabled gamerule must retain native guard");
                level.getGameRules().getRule(net.mcreator.thedeepvoid.init.TheDeepVoidModGameRules.SPAWN_HOLE_STRUCTURE)
                        .set(true, level.getServer());
                net.mcreator.thedeepvoid.procedures.CultTemplePlacementProcedure.execute(level);
                helper.assertTrue(variables.CultTemplePlaced && level.getBlockState(sample.pos()).equals(sample.state()),
                        "Native template must place at -128, old=" + old);
                level.setBlock(sample.pos(), net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState(), 3);
                net.mcreator.thedeepvoid.procedures.CultTemplePlacementProcedure.execute(level);
                helper.assertTrue(level.getBlockState(sample.pos()).is(net.minecraft.world.level.block.Blocks.BARRIER),
                        "Existing placement flag must prevent replacement");
            }
            // Both native one-tick callbacks read the height again: keep the fixture config until they run.
            level.setBlock(new BlockPos(-8, -127, -8), net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState(), 3);
            level.setBlock(new BlockPos(-12, -127, -12), net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState(), 3);
            helper.runAfterDelay(3, () -> {
                try {
                    helper.assertTrue(level.getBlockState(new BlockPos(-8, -127, -8)).isAir(), "Old delayed opening at bottom+1");
                    helper.assertTrue(level.getBlockState(new BlockPos(-12, -127, -12)).isAir(), "New delayed opening at bottom+1");
                    helper.succeed();
                } finally {
                    restore.run();
                }
            });
        } catch (Throwable failure) {
            restore.run();
            throw failure;
        }
    }

    private static com.electronwill.nightconfig.core.CommentedConfig copyConfig(
            final com.electronwill.nightconfig.core.UnmodifiableConfig source) {
        final var copy = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        for (var entry : source.entrySet()) {
            final Object value = entry.getValue();
            copy.set(java.util.List.of(entry.getKey()),
                    value instanceof com.electronwill.nightconfig.core.UnmodifiableConfig section
                            ? copyConfig(section) : value);
        }
        return copy;
    }

    private static void assertDecision(final GameTestHelper helper, final ServerLevel level,
                                       final double y, final boolean expected) {
        // An untracked non-ServerPlayer avoids network/world transfer while retaining native capabilities.
        final Player player = new Player(level, BlockPos.ZERO, 0.0F,
                new GameProfile(UUID.randomUUID(), "VoidBoundary")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
        try {
            final var variables = player.getCapability(TheDeepVoidModVariables.PLAYER_VARIABLES_CAPABILITY)
                    .orElseThrow(() -> new IllegalStateException("Native Deep Void player capability is missing"));
            player.setPos(0.0D, y, 0.0D);
            IfPlayerFallsInVoidProcedure.execute(player.getX(), player.getZ(), player);
            helper.assertTrue(variables.naturalTeleport == expected,
                    "Native fall decision mismatch in " + level.dimension().location() + " at Y=" + y
                            + " (bottom " + level.getMinBuildHeight() + ", expected " + expected + ")");
        } finally {
            player.invalidateCaps();
        }
    }
}
