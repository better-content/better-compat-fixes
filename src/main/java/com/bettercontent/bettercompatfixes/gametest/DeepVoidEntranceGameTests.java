package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.deepvoid.DeepVoidCompassSearch;
import com.bettercontent.bettercompatfixes.deepvoid.DeepVoidEntrancePiece;
import com.bettercontent.bettercompatfixes.deepvoid.DeepVoidEntranceStructure;
import com.bettercontent.bettercompatfixes.deepvoid.DeepVoidStructureLevel;
import com.bettercontent.bettercompatfixes.deepvoid.DeepVoidStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class DeepVoidEntranceGameTests {
    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "scattered_void", timeoutTicks = 400)
    public static void allVariantsHaveSavedChunkClippedExtendedBottomBores(GameTestHelper helper) {
        final var level = helper.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
                new ResourceLocation(BetterContentFixes.MOD_ID, "deep_void_placement_test")));
        helper.assertTrue(level != null && level.getMinBuildHeight() == -128, "Required extended-bottom fixture");
        int index = 0;
        for (String name : DeepVoidEntranceStructure.TEMPLATES) {
            BlockPos bore = new BlockPos(128 + index++ * 64, -128, 128);
            var piece = new DeepVoidEntrancePiece(level.getStructureManager(), new ResourceLocation("the_deep_void", name), bore);
            var restored = new DeepVoidEntrancePiece(level.getStructureManager(), piece.createTag(StructurePieceSerializationContext.fromLevel(level)));
            helper.assertTrue(restored.bore().equals(bore) && restored.getBoundingBox().equals(piece.getBoundingBox()), "Saved piece fidelity: " + name);
            var bounds = piece.getBoundingBox();
            for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
                for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                    var clip = new BoundingBox(cx * 16, -128, cz * 16, cx * 16 + 15, level.getMaxBuildHeight() - 1, cz * 16 + 15);
                    // Sentinel just outside the current chunk must survive each independent placement.
                    BlockPos sentinel = new BlockPos(cx * 16 + 16, -128, cz * 16);
                    level.setBlock(sentinel, Blocks.BARRIER.defaultBlockState(), 2);
                    piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                            RandomSource.create(42), clip, new ChunkPos(cx, cz), bore);
                    helper.assertTrue(level.getBlockState(sentinel).is(Blocks.BARRIER), "Cross-chunk write: " + name);
                }
            }
            for (int y = bore.getY(); y <= bounds.maxY(); y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                helper.assertTrue(level.getBlockState(bore.offset(x, y - bore.getY(), z)).isAir(), "Blocked bore: " + name);
            }
            for (BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
                helper.assertTrue(!level.getBlockState(pos).is(Blocks.STRUCTURE_BLOCK), "Authoring block escaped filter: " + name);
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void nativeStartGenerationUsesExtendedFloorAndRejectsOtherDimensions(GameTestHelper helper) {
        final var level = helper.getLevel();
        final var generator = level.getChunkSource().getGenerator();
        final var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).getHolderOrThrow(
                ResourceKey.create(Registries.STRUCTURE_SET, new ResourceLocation(BetterContentFixes.MOD_ID, "deep_void_entrances")));
        final var placement = (RandomSpreadStructurePlacement) set.value().placement();
        final var state = net.minecraft.world.level.chunk.ChunkGeneratorStructureState.createForFlat(
                level.getChunkSource().randomState(), level.getSeed(), generator.getBiomeSource(), java.util.stream.Stream.of(set));
        final var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(DeepVoidStructures.KEY).value();
        final var other = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
                new ResourceLocation(BetterContentFixes.MOD_ID, "deep_void_placement_test")));
        final var seen = new java.util.HashSet<String>();
        for (int i = 0; i < 360; i++) {
            final var candidate = placement.getPotentialStructureChunk(level.getSeed(), i * placement.spacing(), -placement.spacing());
            final var chunk = new net.minecraft.world.level.chunk.ProtoChunk(candidate, net.minecraft.world.level.chunk.UpgradeData.EMPTY,
                    net.minecraft.world.level.LevelHeightAccessor.create(-128, 384),
                    level.registryAccess().registryOrThrow(Registries.BIOME), null);
            generator.createStructures(level.registryAccess(), state, level.structureManager(), chunk, level.getStructureManager());
            var start = chunk.getStartForStructure(structure);
            helper.assertTrue(start != null && start.isValid() && start.getPieces().size() == 1, "Native scattered structure start");
            var piece = (DeepVoidEntrancePiece) start.getPieces().get(0);
            helper.assertTrue(piece.bore().getY() == -128 && piece.bore().getX() == candidate.getMiddleBlockX()
                    && piece.bore().getZ() == candidate.getMiddleBlockZ(), "Start/compass anchor must use actual bottom");
            seen.add(piece.createTag(StructurePieceSerializationContext.fromLevel(level)).getString("Template"));
            if (i == 0) {
                var foreign = new net.minecraft.world.level.chunk.ProtoChunk(candidate, net.minecraft.world.level.chunk.UpgradeData.EMPTY,
                        other, level.registryAccess().registryOrThrow(Registries.BIOME), null);
                // Same generator, seed, biome and placement; only the dimension manager changes.
                generator.createStructures(level.registryAccess(), state, other.structureManager(), foreign, level.getStructureManager());
                var rejected = foreign.getStartForStructure(structure);
                helper.assertTrue(rejected == null || !rejected.isValid(), "Overworld-biome custom dimension must not get entrances");
            }
        }
        helper.assertTrue(seen.size() == DeepVoidEntranceStructure.TEMPLATES.size(), "Native deterministic selection must exercise every equal-weight candidate");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", timeoutTicks = 300)
    public static void asynchronousCompassQueryConfirmsStartsWithoutTerrain(GameTestHelper helper) {
        final var level = helper.getLevel();
        final var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(DeepVoidStructures.KEY).value();
        final var candidate = new ChunkPos(50000, 50000);
        final var metadata = level.getChunkSource().getChunkFuture(candidate.x, candidate.z,
                net.minecraft.world.level.chunk.ChunkStatus.STRUCTURE_STARTS, true);
        final var result = metadata.thenComposeAsync(loaded -> {
            var chunk = loaded.left().orElseThrow();
            helper.assertTrue(!chunk.getStatus().isOrAfter(net.minecraft.world.level.chunk.ChunkStatus.BIOMES),
                    "Compass query must not populate biomes/terrain");
            // Author an old-chunk negative fixture, then create a real start through native Structure.generate.
            chunk.setStartForStructure(structure, net.minecraft.world.level.levelgen.structure.StructureStart.INVALID_START);
            return DeepVoidCompassSearch.requestStart(level, candidate, structure).thenComposeAsync(absent -> {
                helper.assertTrue(absent.isEmpty(), "Existing chunk without start must retain normal compass behavior");
                var generator = level.getChunkSource().getGenerator();
                var start = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(),
                        level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(), candidate,
                        0, chunk, biome -> true);
                helper.assertTrue(start.isValid(), "Native start fixture must be valid");
                chunk.setStartForStructure(structure, start);
                return DeepVoidCompassSearch.requestStart(level, candidate, structure);
            }, level.getServer());
        }, level.getServer());
        helper.succeedWhen(() -> {
            helper.assertTrue(result.isDone(), "Waiting for asynchronous metadata lookup");
            var target = result.getNow(java.util.Optional.empty());
            helper.assertTrue(target.isPresent() && target.get().equals(new BlockPos(candidate.getMiddleBlockX(),
                    level.getMinBuildHeight(), candidate.getMiddleBlockZ())), "Confirmed start must target its bore");
            helper.assertTrue(!metadata.getNow(null).left().orElseThrow().getStatus().isOrAfter(
                    net.minecraft.world.level.chunk.ChunkStatus.BIOMES), "Lookup must still have generated no terrain");
        });
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void compassBoundariesAndRegisteredStructurePolicy(GameTestHelper helper) {
        var plain = new ItemStack(Items.COMPASS);
        helper.assertTrue(DeepVoidCompassSearch.eligible(plain, Level.OVERWORLD, -0.001), "Below-zero plain compass");
        helper.assertTrue(!DeepVoidCompassSearch.eligible(plain, Level.OVERWORLD, 0), "Y=0 must retain spawn");
        helper.assertTrue(!DeepVoidCompassSearch.eligible(plain, Level.NETHER, -64), "Other dimensions remain native");
        helper.assertTrue(!DeepVoidCompassSearch.eligible(new ItemStack(Items.RECOVERY_COMPASS), Level.OVERWORLD, -64), "Recovery compasses unchanged");
        plain.getOrCreateTag().putString("LodestoneDimension", "minecraft:overworld");
        helper.assertTrue(!DeepVoidCompassSearch.eligible(plain, Level.OVERWORLD, -64), "Lodestone compasses unchanged");
        helper.assertTrue(((DeepVoidStructureLevel) helper.getLevel().structureManager()).better_compat_fixes$isOverworld(), "Overworld manager bridge");
        helper.assertTrue(!((DeepVoidStructureLevel) helper.getLevel().getServer().getLevel(Level.END).structureManager()).better_compat_fixes$isOverworld(), "End dimension exclusion");
        var holder = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(DeepVoidStructures.KEY);
        helper.assertTrue(holder.value() instanceof DeepVoidEntranceStructure, "Registered /locate structure identity");
        var origin = new BlockPos(-100, -100, -100);
        var candidates = DeepVoidCompassSearch.candidates(1, origin, new RandomSpreadStructurePlacement(128, 64, RandomSpreadType.LINEAR, 1739820913));
        helper.assertTrue(candidates.size() == 289 && candidates.stream().distinct().count() == 289, "Bounded unique candidate search");
        long previous = -1;
        for (ChunkPos pos : candidates) {
            long distance = DeepVoidCompassSearch.horizontalDistanceSquared(origin, new BlockPos(pos.getMiddleBlockX(), 0, pos.getMiddleBlockZ()));
            helper.assertTrue(distance >= previous, "Nearest-first candidate order");
            previous = distance;
        }
        helper.succeed();
    }
}
