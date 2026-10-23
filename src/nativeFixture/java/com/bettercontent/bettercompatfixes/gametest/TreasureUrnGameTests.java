package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.TreasureUrnPending;
import com.bettercontent.bettercompatfixes.nativefixture.UrnHiddenItemsAccessor;
import com.bettercontent.bettercompatfixes.nativefixture.UrnOracleRegistration;
import com.bettercontent.bettercompatfixes.nativefixture.UrnRandomScope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.mehvahdjukaar.supplementaries.common.block.blocks.UrnBlock;
import net.mehvahdjukaar.supplementaries.common.block.tiles.UrnBlockTile;
import net.mehvahdjukaar.supplementaries.configs.CommonConfigs;
import net.mehvahdjukaar.supplementaries.reg.ModCreativeTabs;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Nonshipping required native contract, not a replacement feature/worldgen or loot simulator. */
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TreasureUrnGameTests {
    @SubscribeEvent public static void register(RegisterGameTestsEvent event) { event.register(TreasureUrnGameTests.class); }
    private static void require(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
    private static BlockState urn(boolean treasure, boolean waterlogged) {
        return ModRegistry.URN.get().defaultBlockState().setValue(UrnBlock.TREASURE, treasure).setValue(UrnBlock.WATERLOGGED, waterlogged);
    }
    private static ProtoChunk proto(ServerLevel level, ChunkPos pos) {
        ProtoChunk proto = new ProtoChunk(pos, UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        proto.setStatus(ChunkStatus.FEATURES);
        return proto;
    }
    private static CompoundTag dummy(BlockPos pos) {
        CompoundTag tag = new CompoundTag(); tag.putString("id", "DUMMY");
        tag.putInt("x", pos.getX()); tag.putInt("y", pos.getY()); tag.putInt("z", pos.getZ()); return tag;
    }

    private static final class NativeAccessProbe extends ProtoChunk {
        boolean nativeLookup;
        NativeAccessProbe(ServerLevel level, ChunkPos pos) {
            super(pos, UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(Registries.BIOME), null);
            setStatus(ChunkStatus.FEATURES);
        }
        @Override public BlockEntity getBlockEntity(BlockPos pos) { nativeLookup = true; return null; }
        @Override public CompoundTag getBlockEntityNbt(BlockPos pos) { throw new UnsupportedOperationException("native accessor probe"); }
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "treasure_urn", timeoutTicks = 400)
    public static void nativeWorldGenRegionPreservesTreasureDummyWithoutPromotionAndOtherFactories(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkPos cp = new ChunkPos(helper.absolutePos(new BlockPos(1, 2, 1)));
        ProtoChunk proto = proto(level, cp);
        WorldGenRegion region = new WorldGenRegion(level, List.of(proto), ChunkStatus.FEATURES, 0);
        int y = helper.absolutePos(new BlockPos(1, 2, 1)).getY();
        List<BlockPos> treasurePositions = new ArrayList<>();
        List<BlockPos> normalPositions = new ArrayList<>();
        for (int wet = 0; wet < 2; wet++) {
            BlockPos treasure = new BlockPos(cp.getMinBlockX() + 2 + wet * 2, y, cp.getMinBlockZ() + 2);
            BlockPos normal = treasure.offset(0, 0, 2);
            BlockState treasureState = urn(true, wet == 1), normalState = urn(false, wet == 1);
            require(treasureState.hasBlockEntity() && normalState.hasBlockEntity(), "Global native EntityBlock predicate must remain true");
            require(((UrnBlock) treasureState.getBlock()).newBlockEntity(treasure, treasureState) == null, "Treasure factory must remain null");
            require(UrnOracleRegistration.original().defaultBlockState().setValue(UrnBlock.TREASURE, true).hasBlockEntity(),
                    "Unrelated original oracle block must keep vanilla EntityBlock classification");
            require(region.setBlock(treasure.below(), Blocks.STONE.defaultBlockState(), 2, 512), "Native support placement failed");
            require(region.setBlock(normal.below(), Blocks.STONE.defaultBlockState(), 2, 512), "Native support placement failed");
            require(region.setBlock(treasure, treasureState, 2, 512), "Native treasure producer placement failed");
            require(region.setBlock(normal, normalState, 2, 512), "Native ordinary producer placement failed");
            CompoundTag treasureMarker = dummy(treasure);
            require(treasureMarker.equals(proto.getBlockEntityNbt(treasure)) && proto.getBlockEntity(treasure) == null, "Native producer marker changed");
            require(region.getBlockEntity(treasure) == null && treasureMarker.equals(proto.getBlockEntityNbt(treasure)), "Native region lookup promoted/removed synthetic marker");
            require(TreasureUrnPending.minimalDummy(proto.getBlockEntityNbt(normal), normal), "Ordinary urn lost native pending DUMMY");
            if (wet == 0) require(region.getBlockEntity(normal) instanceof UrnBlockTile, "Ordinary region lookup lost its native tile");
            treasurePositions.add(treasure); normalPositions.add(normal);
        }
        BlockPos chest = new BlockPos(cp.getMinBlockX() + 8, y, cp.getMinBlockZ() + 2), barrel = chest.offset(0, 0, 2);
        require(region.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2, 512), "Native chest placement failed");
        require(region.setBlock(barrel, Blocks.BARREL.defaultBlockState(), 2, 512), "Native barrel placement failed");
        require(TreasureUrnPending.minimalDummy(proto.getBlockEntityNbt(chest), chest), "Unrelated chest pending changed");
        require(TreasureUrnPending.minimalDummy(proto.getBlockEntityNbt(barrel), barrel), "Unrelated barrel pending changed");
        require(region.getBlockEntity(chest) instanceof ChestBlockEntity, "Unrelated region chest lookup failed");
        BlockPos absent = new BlockPos(cp.getMinBlockX() + 10, y, cp.getMinBlockZ() + 10);
        proto.setBlockState(absent, urn(true, false), false);
        require(region.getBlockEntity(absent) == null && proto.getBlockEntityNbt(absent) == null, "Absent-pending region lookup invented BE/tag");
        NativeAccessProbe unsupported = new NativeAccessProbe(level, cp);
        WorldGenRegion unsupportedRegion = new WorldGenRegion(level, List.of(unsupported), ChunkStatus.FEATURES, 0);
        try { unsupportedRegion.getBlockEntity(absent); throw new AssertionError("Unknown chunk accessor failure was hidden"); }
        catch (UnsupportedOperationException nativeFailure) {
            require(unsupported.nativeLookup && nativeFailure.getMessage().equals("native accessor probe"), "Unsupported chunk did not preserve original access order/exception");
        }
        LevelChunk promoted = new LevelChunk(level, proto, null);
        require(promoted.getBlockEntity(absent) == null && promoted.getBlockEntityNbt(absent) == null, "Absent-pending LevelChunk lookup invented BE/tag");
        for (BlockPos pos : treasurePositions) {
            require(promoted.getBlockEntity(pos) == null && dummy(pos).equals(promoted.getBlockEntityNbt(pos)), "Level lookup changed native treasure marker");
            CompoundTag packed = dummy(pos); packed.putBoolean("keepPacked", true);
            require(packed.equals(promoted.getBlockEntityNbtForSaving(pos)) && dummy(pos).equals(promoted.getBlockEntityNbt(pos)), "Native prepostprocess save changed marker");
        }
        for (BlockPos pos : normalPositions) require(promoted.getBlockEntity(pos) instanceof UrnBlockTile, "Normal urn failed native promotion");
        require(promoted.getBlockEntity(chest) instanceof ChestBlockEntity && promoted.getBlockEntity(barrel) instanceof BarrelBlockEntity,
                "Unrelated native BE promotion changed");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "treasure_urn", timeoutTicks = 400)
    public static void nativeLegacyMarkerSaveReloadAndNormalInventoryRemainNative(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkPos cp = new ChunkPos(helper.absolutePos(new BlockPos(1, 2, 1)));
        BlockPos pos = new BlockPos(cp.getMinBlockX() + 2, helper.absolutePos(new BlockPos(1, 2, 1)).getY(), cp.getMinBlockZ() + 2);
        LevelChunk legacy = new LevelChunk(level, cp);
        legacy.setBlockState(pos, urn(true, false), false);
        CompoundTag marker = dummy(pos); legacy.setBlockEntityNbt(marker);
        UrnBlockTile preexisting = new UrnBlockTile(pos, urn(true, false)); preexisting.setLevel(level);
        legacy.getBlockEntities().put(pos, preexisting); // Seed an already-present legacy object, never a production repair.
        try {
            require(legacy.getBlockEntity(pos) == preexisting, "Existing live BE must bypass synthetic-marker guard");
            require(marker.equals(legacy.getBlockEntityNbt(pos)), "Existing live BE control lost pending data");
        } finally { legacy.getBlockEntities().remove(pos); }
        WorldGenRegion fullRegion = new WorldGenRegion(level, List.of(legacy), ChunkStatus.FEATURES, 0);
        require(fullRegion.getBlockEntity(pos) == null && marker.equals(legacy.getBlockEntityNbt(pos)), "Region cached LevelChunk branch materialized/removed marker");
        WorldGenRegion imposterRegion = new WorldGenRegion(level, List.of(new ImposterProtoChunk(legacy, false)), ChunkStatus.FEATURES, 0);
        require(imposterRegion.getBlockEntity(pos) == null && marker.equals(legacy.getBlockEntityNbt(pos)), "Pinned imposter wrapper branch changed marker");
        for (var mode : LevelChunk.EntityCreationType.values()) {
            require(legacy.getBlockEntity(pos, mode) == null, "Stateless legacy lookup returned BE");
            require(marker.equals(legacy.getBlockEntityNbt(pos)), "Lookup mutated/removed legacy marker");
        }
        CompoundTag packed = legacy.getBlockEntityNbtForSaving(pos);
        CompoundTag expectedPacked = marker.copy(); expectedPacked.putBoolean("keepPacked", true);
        require(packed.equals(expectedPacked) && marker.equals(legacy.getBlockEntityNbt(pos)), "Native save fallback did not copy/preserve marker");
        CompoundTag serialized = ChunkSerializer.write(level, legacy);
        require(serialized.getList("block_entities", Tag.TAG_COMPOUND).contains(expectedPacked), "Native chunk serializer lost pending marker");
        ProtoChunk read = ChunkSerializer.read(level, level.getPoiManager(), cp, serialized);
        require(read instanceof ImposterProtoChunk, "Full chunk reload did not use native imposter wrapper");
        LevelChunk loaded = ((ImposterProtoChunk) read).getWrapped(); loaded.runPostLoad();
        require(expectedPacked.equals(loaded.getBlockEntityNbt(pos)), "Native keepPacked load changed pending bytes");
        require(loaded.getBlockEntity(pos) == null && expectedPacked.equals(loaded.getBlockEntityNbt(pos)), "Reloaded lookup mutated pending marker");
        loaded.postProcessGeneration(); // Original clear remains unmodified, consuming synthetic markers as before.
        require(loaded.getBlockEntity(pos) == null && loaded.getBlockEntityNbt(pos) == null, "Native postprocessing did not consume synthetic marker normally");
        require(loaded.getBlockState(pos).equals(urn(true, false)), "Native postprocessing changed treasure state");
        // Genuine authored treasure inventories MUST continue through the actual native decoder, save, and state transitions.
        UrnBlockTile authored = new UrnBlockTile(pos, urn(true, false)); authored.setItem(0, new ItemStack(Items.EMERALD, 5));
        CompoundTag authoredTag = authored.saveWithFullMetadata();
        legacy.setBlockEntityNbt(authoredTag.copy());
        require(legacy.getBlockEntity(pos) instanceof UrnBlockTile, "Real treasure BE was intercepted or rejected");
        UrnBlockTile liveAuthored = (UrnBlockTile) legacy.getBlockEntity(pos);
        require(liveAuthored.getItem(0).is(Items.EMERALD) && liveAuthored.getItem(0).getCount() == 5, "Authored treasure inventory decode lost contents");
        require(legacy.getBlockEntities().get(pos) == liveAuthored && fullRegion.getBlockEntity(pos) == liveAuthored
                        && imposterRegion.getBlockEntity(pos) == liveAuthored,
                "Real treasure BE must be retained in native live map and cached full-region path");
        legacy.setBlockState(pos, urn(false, false), false);
        require(legacy.getBlockEntity(pos) == liveAuthored && liveAuthored.getItem(0).getCount() == 5, "Treasure->ordinary transition lost real tile");
        legacy.setBlockState(pos, urn(true, false), false);
        require(legacy.getBlockEntity(pos) == liveAuthored && liveAuthored.getItem(0).getCount() == 5, "Ordinary->treasure transition lost real tile");
        var authoredArchive = ChunkSerializer.write(level, legacy);
        LevelChunk authoredReload = ((ImposterProtoChunk) ChunkSerializer.read(level, level.getPoiManager(), cp, authoredArchive)).getWrapped();
        authoredReload.runPostLoad();
        require(authoredReload.getBlockEntity(pos) instanceof UrnBlockTile
                && ((UrnBlockTile) authoredReload.getBlockEntity(pos)).getItem(0).getCount() == 5, "Real treasure inventory native save/full reload failed");
        ProtoChunk authoredProto = proto(level, cp); authoredProto.setBlockState(pos, urn(true, false), false);
        authoredProto.setBlockEntityNbt(authoredTag.copy());
        WorldGenRegion authoredRegion = new WorldGenRegion(level, List.of(authoredProto), ChunkStatus.FEATURES, 0);
        require(authoredRegion.getBlockEntity(pos) instanceof UrnBlockTile, "Region real treasure decoder was intercepted");
        require(((UrnBlockTile) authoredProto.getBlockEntities().get(pos)).getItem(0).getCount() == 5, "Region real treasure contents lost");
        ProtoChunk protoReload = ChunkSerializer.read(level, level.getPoiManager(), cp, ChunkSerializer.write(level, authoredProto));
        LevelChunk promotedReal = new LevelChunk(level, protoReload, null);
        require(promotedReal.getBlockEntity(pos) instanceof UrnBlockTile
                && ((UrnBlockTile) promotedReal.getBlockEntity(pos)).getItem(0).getCount() == 5, "Region authored save/proto reload/promotion lost inventory");
        // Real/extra/malformed data is not admitted or rewritten; no invalid decode is manufactured in an error-clean native suite.
        CompoundTag real = dummy(pos); real.putString("id", "supplementaries:urn"); real.putString("fixture_payload", "preserved");
        legacy.setBlockEntityNbt(real);
        require(!TreasureUrnPending.minimalDummy(real, pos), "Real data admitted as synthetic");
        require(legacy.getBlockState(pos).hasBlockEntity() && legacy.getBlockEntity(pos) == liveAuthored
                && real.equals(legacy.getBlockEntityNbt(pos)), "Existing live BE/state query changed native predicate/authored data");
        CompoundTag malformed = dummy(pos); malformed.putString("x", Integer.toString(pos.getX())); legacy.setBlockEntityNbt(malformed);
        require(!TreasureUrnPending.minimalDummy(malformed, pos) && legacy.getBlockEntity(pos) == liveAuthored
                && malformed.equals(legacy.getBlockEntityNbt(pos)), "Malformed pending tag/live BE was coerced/deleted/intercepted");
        // Normal inventory persistence uses actual registered native tile and chunk serializer, not an empty replacement.
        BlockPos normal = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(normal.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(normal, urn(false, false), 3);
        require(level.getBlockEntity(normal) instanceof UrnBlockTile, "Normal urn has no inventory tile");
        UrnBlockTile tile = (UrnBlockTile) level.getBlockEntity(normal);
        tile.setItem(0, new ItemStack(Items.DIAMOND, 3));
        CompoundTag saved = tile.saveWithFullMetadata();
        BlockEntity restored = BlockEntity.loadStatic(normal, urn(false, false), saved);
        require(restored instanceof UrnBlockTile && ((UrnBlockTile) restored).getItem(0).getCount() == 3, "Native inventory save/reload lost items");
        level.removeBlockEntity(normal); level.setBlockEntity(restored);
        AABB area = new AABB(normal).inflate(1);
        require(level.getEntitiesOfClass(ItemEntity.class, area).isEmpty(), "Native inventory drop control contaminated");
        level.removeBlock(normal, false); // Native onRemove is the only producer here.
        int count = level.getEntitiesOfClass(ItemEntity.class, area).stream().filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum();
        require(count == 3, "Ordinary inventory must drop exactly once on removal");
        level.removeBlock(normal, false);
        require(level.getEntitiesOfClass(ItemEntity.class, area).stream().filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum() == 3,
                "Repeated native removal duplicated contents");
        level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
        helper.succeed();
    }

    private record Roll(List<CompoundTag> items, float luck, int contexts) { }
    private static Roll roll(Block block, BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, long seed) {
        LootParams.Builder builder = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, tool).withLuck(0.5F);
        try (UrnRandomScope scope = new UrnRandomScope(seed)) {
            List<CompoundTag> items = block.getDrops(state, builder).stream().map(stack -> stack.save(new CompoundTag())).toList();
            return new Roll(items, scope.luck, scope.lootContexts);
        }
    }
    private static List<String> spawn(Block block, BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, long seed) {
        try (UrnRandomScope scope = new UrnRandomScope(seed)) {
            block.spawnAfterBreak(state, level, pos, tool, true);
            return scope.spawned.stream().map(entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()).toList();
        }
    }
    private static void verifyOriginalLootResources(ServerLevel level) throws Exception {
        for (String path : List.of("urn.json", "urn_loot/common.json", "urn_loot/uncommon.json", "urn_loot/rare.json", "urn_loot/epic.json", "urn_loot/urn_loot.json")) {
            String relative = "data/supplementaries/loot_tables/blocks/" + path;
            try (InputStream raw = TreasureUrnGameTests.class.getResourceAsStream("/urn-oracle/" + relative);
                 InputStream actual = level.getServer().getResourceManager().getResource(new ResourceLocation("supplementaries", "loot_tables/blocks/" + path)).orElseThrow().open()) {
                require(raw != null && Arrays.equals(raw.readAllBytes(), actual.readAllBytes()), "Active urn loot resources diverged from independent raw pin: " + path);
            }
        }
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "treasure_urn", timeoutTicks = 400)
    public static void nativeTreasureLootFortuneHiddenRerollSilkUseAndSpawnMatchOriginal(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel(); BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockState state = urn(true, false); Block actual = state.getBlock(), original = UrnOracleRegistration.original();
        verifyOriginalLootResources(level);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3); level.setBlock(pos, state, 3);
        require(level.getBlockEntity(pos) == null && state.hasBlockEntity(), "Treasure factory stays stateless while original global predicate remains true");
        require(state.getAnalogOutputSignal(level, pos) == 15 && original.getAnalogOutputSignal(state, level, pos) == 15, "Treasure comparator changed");
        var player = helper.makeMockPlayer();
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        require(state.use(level, player, InteractionHand.MAIN_HAND, hit) == InteractionResult.PASS
                && original.use(state, level, pos, player, InteractionHand.MAIN_HAND, hit) == InteractionResult.PASS, "Treasure exposed empty storage/use changed");
        var hidden = UrnHiddenItemsAccessor.betterContent$hiddenItems();
        var ash = BuiltInRegistries.ITEM.get(new ResourceLocation("supplementaries", "ash"));
        boolean wasHidden = hidden.contains(ash); hidden.add(ash);
        boolean rerolled = false, silkDroppedUrn = false;
        try {
            for (int fortune : new int[]{0, 3}) for (boolean silk : new boolean[]{false, true}) {
                ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
                if (fortune > 0) tool.enchant(Enchantments.BLOCK_FORTUNE, fortune);
                if (silk) tool.enchant(Enchantments.SILK_TOUCH, 1);
                for (long seed = 1; seed <= 128; seed++) {
                    Roll baseline = roll(original, state, level, pos, tool, seed), repaired = roll(actual, state, level, pos, tool, seed);
                    require(baseline.equals(repaired), "Seeded native original/repaired loot/luck/reroll mismatch");
                    require(Float.compare(repaired.luck, 0.5F + fortune * 0.25F) == 0, "Native no-BE Fortune luck branch bypassed");
                    require(repaired.contexts > 0, "Native loot RNG witness missing"); rerolled |= repaired.contexts > 1;
                    for (CompoundTag item : repaired.items) {
                        ItemStack stack = ItemStack.of(item);
                        require(!ModCreativeTabs.isHidden(stack.getItem()), "Hidden item escaped native filter");
                        if (stack.is(actual.asItem())) { require(silk, "Urn item dropped without Silk Touch"); silkDroppedUrn = true; }
                    }
                }
            }
            require(rerolled && silkDroppedUrn, "Native hidden reroll / Silk Touch controls were not actually exercised");
        } finally { if (!wasHidden) hidden.remove(ash); }
        double chance = CommonConfigs.Functional.URN_ENTITY_SPAWN_CHANCE.get();
        long eligibleSeed = 1;
        while (eligibleSeed < 100000 && !(RandomSource.create(eligibleSeed).nextFloat() < chance)) eligibleSeed++;
        require(eligibleSeed < 100000, "Native spawn-chance positive control unavailable");
        boolean drops = level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
        try {
            level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(true, level.getServer());
            List<String> expected = spawn(original, state, level, pos, ItemStack.EMPTY, eligibleSeed);
            require(!expected.isEmpty() && expected.equals(spawn(actual, state, level, pos, ItemStack.EMPTY, eligibleSeed)), "Native treasure spawn positive changed");
            ItemStack silkTool = new ItemStack(Items.DIAMOND_PICKAXE); silkTool.enchant(Enchantments.SILK_TOUCH, 1);
            require(spawn(original, state, level, pos, silkTool, eligibleSeed).isEmpty()
                    && spawn(actual, state, level, pos, silkTool, eligibleSeed).isEmpty(), "Silk Touch must suppress native treasure creature");
            level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(false, level.getServer());
            require(spawn(original, state, level, pos, ItemStack.EMPTY, eligibleSeed).isEmpty()
                    && spawn(actual, state, level, pos, ItemStack.EMPTY, eligibleSeed).isEmpty(), "Native doBlockDrops denial changed");
        } finally { level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(drops, level.getServer()); }
        helper.succeed();
    }
}
