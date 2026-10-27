package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual transformed upstream entry points, using explicitly pinned source-only dependencies. */
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class MigratedCompatRegressionGameTests {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (!Boolean.getBoolean("better_compat_fixes.migratedRegressionFixture")) return;
        for (String id : List.of("cataclysm", "the_flesh_that_hates", "untamedwilds")) {
            if (!ModList.get().isLoaded(id)) throw new IllegalStateException("Missing regression fixture: " + id);
        }
        event.register(MigratedCompatRegressionGameTests.class);
        event.register(com.bettercontent.bettercompatfixes.compat.UntamedClientEntityDataGameTests.class);
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "migrated_compat")
    public static void fleshSavedDataLoadsPrintedKeysAndRewritesPackedKeys(GameTestHelper helper) throws Exception {
        CompoundTag saved;
        try (java.io.InputStream input = MigratedCompatRegressionGameTests.class.getResourceAsStream(
                "/regressions/flesh-spread-printed-keys.dat")) {
            if (input == null) throw new IllegalStateException("Missing local printed-key SavedData fixture");
            saved = net.minecraft.nbt.NbtIo.readCompressed(input).getCompound("data");
        }
        BlockPos key = new BlockPos(100121, 54, 100043);
        CompoundTag spreadTag = saved.getList("BlocksToSpread", Tag.TAG_COMPOUND).getCompound(0).getCompound("SpreadData");
        helper.assertTrue(spreadTag.getCompound("activeBlockOrigins").contains(key.toString()),
                "Historical fixture must actually contain the legacy printed origin key");
        // The captured save has an empty counter map. Add its second legacy-key boundary,
        // including negative coordinates, without replacing the captured origin regression.
        BlockPos counterKey = new BlockPos(-213, 70, 4097);
        spreadTag.getCompound("requeueCounters").putInt(counterKey.toString(), 7);
        Class<?> dataType = Class.forName("net.mcreator.thefleshthathates.FleshBlockSavedData");
        SavedData loaded = (SavedData) dataType.getMethod("load", CompoundTag.class).invoke(null, saved);
        CompoundTag rewritten = loaded.save(new CompoundTag()).getList("BlocksToSpread", Tag.TAG_COMPOUND)
                .getCompound(0).getCompound("SpreadData");
        String packed = Long.toString(key.asLong());
        helper.assertTrue(rewritten.getCompound("activeBlockOrigins").contains(packed)
                && !rewritten.getCompound("activeBlockOrigins").getBoolean(packed), "Printed origin key and false value must survive actual SavedData load/write");
        helper.assertTrue(rewritten.getCompound("requeueCounters").getInt(Long.toString(counterKey.asLong())) == 7, "Printed counter key must survive actual SavedData load/write");
        helper.assertTrue(!rewritten.getCompound("activeBlockOrigins").contains(key.toString()), "Writer must migrate legacy printed keys");
        SavedData reloaded = (SavedData) dataType.getMethod("load", CompoundTag.class).invoke(null, loaded.save(new CompoundTag()));
        helper.assertTrue(reloaded.save(new CompoundTag()).equals(loaded.save(new CompoundTag())), "Packed SavedData must round-trip stably");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "migrated_compat", timeoutTicks = 400)
    public static void pyramidMarkersRespectCurrentChunkAndNativePlacement(GameTestHelper helper) throws Exception {
        Class<?> pieceType = Class.forName("com.github.L_Ender.cataclysm.structures.Cursed_Pyramid_Structure$Piece");
        TemplateStructurePiece piece = (TemplateStructurePiece) pieceType.getConstructor(
                net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager.class,
                ResourceLocation.class, BlockPos.class, Rotation.class).newInstance(
                helper.getLevel().getStructureManager(), new ResourceLocation("cataclysm", "cursed_pyramid_lower1"),
                // Keep the real template's chunk away from neighboring tiny GameTest fixtures.
                helper.absolutePos(new BlockPos(1024, 2, 1024)), Rotation.NONE);
        Method marker = null;
        for (Method candidate : pieceType.getDeclaredMethods()) {
            if (java.util.Arrays.equals(candidate.getParameterTypes(), new Class<?>[] {
                    String.class, BlockPos.class, ServerLevelAccessor.class, RandomSource.class, BoundingBox.class})) marker = candidate;
        }
        if (marker == null) throw new IllegalStateException("Pinned pyramid marker callback missing");
        marker.setAccessible(true);
        BlockPos inside = helper.absolutePos(new BlockPos(2, 3, 2));
        ChunkPos chunk = new ChunkPos(inside);
        BoundingBox bounds = new BoundingBox(chunk.getMinBlockX(), helper.getLevel().getMinBuildHeight(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), helper.getLevel().getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
        BlockPos outside = inside.offset(16, 0, 0);
        for (BlockPos pos : List.of(inside, outside)) helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
        marker.invoke(piece, "sus", outside, helper.getLevel(), RandomSource.create(0xC0FFEE), bounds);
        helper.assertTrue(helper.getLevel().getBlockState(outside).is(Blocks.STONE), "Out-of-chunk marker must not write");
        marker.invoke(piece, "sus", inside, helper.getLevel(), RandomSource.create(0xC0FFEE), bounds);
        helper.assertTrue(helper.getLevel().getBlockState(inside).is(Blocks.SUSPICIOUS_SAND), "In-chunk marker must still place native suspicious sand");
        // Real template placement, with fixed RNG, not a synthetic replacement callback.
        BoundingBox box = piece.getBoundingBox();
        ChunkPos placementChunk = new ChunkPos(box.getCenter());
        BoundingBox placementBounds = new BoundingBox(placementChunk.getMinBlockX(), helper.getLevel().getMinBuildHeight(), placementChunk.getMinBlockZ(),
                placementChunk.getMaxBlockX(), helper.getLevel().getMaxBuildHeight() - 1, placementChunk.getMaxBlockZ());
        helper.getLevel().getChunk(placementChunk.x, placementChunk.z);
        java.util.concurrent.atomic.AtomicInteger writes = new java.util.concurrent.atomic.AtomicInteger();
        net.minecraft.world.level.WorldGenLevel boundedLevel = (net.minecraft.world.level.WorldGenLevel) java.lang.reflect.Proxy.newProxyInstance(
                MigratedCompatRegressionGameTests.class.getClassLoader(), new Class<?>[] {net.minecraft.world.level.WorldGenLevel.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setBlock") && args != null && args[0] instanceof BlockPos pos) {
                        helper.assertTrue(placementBounds.isInside(pos), "Native pyramid placement wrote outside the current chunk: " + pos);
                        writes.incrementAndGet();
                    }
                    try { return method.invoke(helper.getLevel(), args); }
                    catch (java.lang.reflect.InvocationTargetException error) { throw error.getCause(); }
                });
        piece.postProcess(boundedLevel, helper.getLevel().structureManager(), helper.getLevel().getChunkSource().getGenerator(),
                RandomSource.create(0xC0FFEE), placementBounds, placementChunk, BlockPos.ZERO);
        helper.assertTrue(writes.get() > 0, "Pinned native pyramid template must actually place blocks");
        helper.succeed();
    }
}
