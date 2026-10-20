package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.SpawnerMiningPolicy;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerTools;

/** Nonshipping native fixture: actual pack tag, registered listeners and real player removal. */
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class SpawnerMiningGameTests {
    private SpawnerMiningGameTests() {
    }

    @SubscribeEvent
    public static void register(final RegisterGameTestsEvent event) {
        event.register(SpawnerMiningGameTests.class);
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void lowerAndUnsuitableToolsCannotRemoveSpawner(final GameTestHelper helper) {
        assertTags(helper);
        final var player = player(helper);
        final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (final var stack : List.of(ItemStack.EMPTY, new ItemStack(Items.WOODEN_PICKAXE),
                new ItemStack(Items.STONE_PICKAXE), new ItemStack(Items.IRON_PICKAXE),
                new ItemStack(Items.GOLDEN_PICKAXE), new ItemStack(Items.DIAMOND_AXE),
                new ItemStack(Items.DIAMOND_SHOVEL))) {
            assertDenied(helper, player, pos, stack);
        }
        // A suitable offhand does not qualify an unsuitable main hand.
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
        assertDenied(helper, player, pos, new ItemStack(Items.IRON_PICKAXE));
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void qualifyingToolsAndCreativeRetainNativeRemoval(final GameTestHelper helper) {
        assertTags(helper);
        final var player = player(helper);
        final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (final var item : List.of(Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE)) {
            final var stack = new ItemStack(item);
            placeSpawner(helper, pos);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            assertEvents(helper, player, pos, false);
            final int beforeXp = xp(helper, pos);
            player.gameMode.destroyBlock(pos);
            helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "Qualifying pickaxe failed native removal");
            helper.assertTrue(stack.getDamageValue() == 1, "Native durability payment changed");
            final int gained = xp(helper, pos) - beforeXp;
            helper.assertTrue(gained >= 15 && gained <= 43, "Native spawner XP changed: " + gained);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, area(pos)).isEmpty(),
                    "Spawner gained an item-drop or collection mechanic");
        }
        placeSpawner(helper, pos);
        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        assertEvents(helper, player, pos, false);
        player.gameMode.destroyBlock(pos);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "Creative bypass failed");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void actualTinkersToolsUseNativeStackCapabilityIncludingBrokenState(final GameTestHelper helper) {
        assertTags(helper);
        final var player = player(helper);
        final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final var iron = tinkersPickaxe("iron");
        final var cobalt = tinkersPickaxe("cobalt");
        final var manyullyn = tinkersPickaxe("manyullyn");
        helper.assertTrue(ToolStack.from(iron).getStats().get(ToolStats.HARVEST_TIER) == Tiers.IRON,
                "Iron fixture did not resolve native iron capability");
        helper.assertTrue(ToolStack.from(cobalt).getStats().get(ToolStats.HARVEST_TIER) == Tiers.DIAMOND,
                "Cobalt fixture did not resolve native diamond capability");
        helper.assertTrue(ToolStack.from(manyullyn).getStats().get(ToolStats.HARVEST_TIER) == Tiers.NETHERITE,
                "Manyullyn fixture did not resolve native higher capability");
        assertDenied(helper, player, pos, iron);
        for (final var stack : List.of(cobalt, manyullyn)) {
            placeSpawner(helper, pos);
            helper.assertTrue(stack.isCorrectToolForDrops(Blocks.SPAWNER.defaultBlockState()), "Native Tinkers capability rejected");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            assertEvents(helper, player, pos, false);
            player.gameMode.destroyBlock(pos);
            helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "Qualifying Tinkers tool failed native removal");
        }
        final var broken = tinkersPickaxe("cobalt");
        final var tool = ToolStack.from(broken);
        tool.setDamage((int) tool.getStats().get(ToolStats.DURABILITY).floatValue());
        helper.assertTrue(tool.isBroken(), "Native Tinkers fixture did not break");
        assertDenied(helper, player, pos, broken);
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void unrelatedBlocksAndPriorCancellationsRemainUnchanged(final GameTestHelper helper) {
        final var player = player(helper);
        final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
        assertEvents(helper, player, pos, false);
        player.gameMode.destroyBlock(pos);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "Unrelated stone mining was denied");
        placeSpawner(helper, pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        final var left = leftClick(player, pos);
        left.setCanceled(true);
        SpawnerMiningPolicy.onLeftClickBlock(left);
        final var speed = new PlayerEvent.BreakSpeed(player, Blocks.SPAWNER.defaultBlockState(), 7.0F, pos);
        speed.setNewSpeed(2.0F);
        speed.setCanceled(true);
        SpawnerMiningPolicy.onBreakSpeed(speed);
        final var broken = new BlockEvent.BreakEvent(helper.getLevel(), pos, Blocks.SPAWNER.defaultBlockState(), player);
        broken.setCanceled(true);
        SpawnerMiningPolicy.onBreakBlock(broken);
        helper.assertTrue(left.isCanceled() && speed.isCanceled() && broken.isCanceled(), "Policy cleared an existing cancellation");
        helper.assertTrue(speed.getNewSpeed() == 2.0F, "Allowed tool overwrote another listener's speed");
        for (final var mode : List.of(GameType.ADVENTURE, GameType.SPECTATOR)) {
            player.setGameMode(mode);
            helper.assertTrue(!player.gameMode.destroyBlock(pos), "Policy granted restricted-mode mining: " + mode);
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.SPAWNER), "Restricted-mode mining removed spawner");
        }
        helper.succeed();
    }

    private static void assertTags(final GameTestHelper helper) {
        final var state = Blocks.SPAWNER.defaultBlockState();
        helper.assertTrue(state.is(BlockTags.NEEDS_DIAMOND_TOOL) && state.is(BlockTags.MINEABLE_WITH_PICKAXE),
                "Actual pack spawner tier fixture is not loaded");
        helper.assertTrue(Blocks.OBSIDIAN.defaultBlockState().is(BlockTags.NEEDS_DIAMOND_TOOL), "Pack tag replaced upstream diamond entries");
    }

    private static ServerPlayer player(final GameTestHelper helper) {
        final var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "spawner_gate"));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void placeSpawner(final GameTestHelper helper, final BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, Blocks.SPAWNER.defaultBlockState());
        final var spawner = (SpawnerBlockEntity) helper.getLevel().getBlockEntity(pos);
        final var tag = spawner.saveWithFullMetadata();
        tag.putShort("Delay", (short) 1234);
        tag.putShort("SpawnCount", (short) 7);
        spawner.load(tag);
    }

    private static void assertDenied(final GameTestHelper helper, final ServerPlayer player, final BlockPos pos, final ItemStack stack) {
        placeSpawner(helper, pos);
        helper.assertTrue(!stack.isCorrectToolForDrops(Blocks.SPAWNER.defaultBlockState()), "Denied fixture unexpectedly qualifies");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final var before = helper.getLevel().getBlockEntity(pos).saveWithFullMetadata();
        final var beforeTool = stack.copy();
        final int beforeXp = xp(helper, pos);
        final int beforeItems = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area(pos)).size();
        assertEvents(helper, player, pos, true);
        helper.assertTrue(!player.gameMode.destroyBlock(pos), "Denied native fake-player removal succeeded");
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.SPAWNER), "Denied attempt removed spawner");
        helper.assertTrue(before.equals(helper.getLevel().getBlockEntity(pos).saveWithFullMetadata()), "Denied attempt changed spawner data");
        helper.assertTrue(ItemStack.matches(beforeTool, stack), "Denied attempt consumed tool durability or item state");
        helper.assertTrue(xp(helper, pos) == beforeXp, "Denied attempt produced XP");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, area(pos)).size() == beforeItems, "Denied attempt produced items");
    }

    private static PlayerInteractEvent.LeftClickBlock leftClick(final ServerPlayer player, final BlockPos pos) {
        return new PlayerInteractEvent.LeftClickBlock(player, pos, Direction.UP, PlayerInteractEvent.LeftClickBlock.Action.START);
    }

    private static void assertEvents(final GameTestHelper helper, final ServerPlayer player, final BlockPos pos, final boolean denied) {
        final var state = helper.getLevel().getBlockState(pos);
        final var abort = new PlayerInteractEvent.LeftClickBlock(player, pos, Direction.UP, PlayerInteractEvent.LeftClickBlock.Action.ABORT);
        MinecraftForge.EVENT_BUS.post(abort);
        helper.assertTrue(!abort.isCanceled(), "Native mining abort cleanup was blocked");
        for (final var action : List.of(PlayerInteractEvent.LeftClickBlock.Action.START,
                PlayerInteractEvent.LeftClickBlock.Action.STOP, PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD)) {
            final var left = new PlayerInteractEvent.LeftClickBlock(player, pos, Direction.UP, action);
            MinecraftForge.EVENT_BUS.post(left);
            helper.assertTrue(left.isCanceled() == denied, "Left-click admission mismatch: " + action);
        }
        final var speed = new PlayerEvent.BreakSpeed(player, state, 7.0F, pos);
        MinecraftForge.EVENT_BUS.post(speed);
        helper.assertTrue(speed.isCanceled() == denied, "Break-speed admission mismatch");
        if (denied) helper.assertTrue(speed.getNewSpeed() == 0.0F, "Denied mining retained progress");
        final var broken = new BlockEvent.BreakEvent(helper.getLevel(), pos, state, player);
        MinecraftForge.EVENT_BUS.post(broken);
        helper.assertTrue(broken.isCanceled() == denied, "Authoritative block-break admission mismatch");
    }

    private static ItemStack tinkersPickaxe(final String head) {
        final var materials = MaterialNBT.builder().add(MaterialVariantId.parse("tconstruct:" + head))
                .add(MaterialVariantId.parse("tconstruct:wood")).add(MaterialVariantId.parse("tconstruct:wood")).build();
        final var item = TinkerTools.pickaxe.get();
        return ToolStack.createTool(item, item.getToolDefinition(), materials).createStack();
    }

    private static AABB area(final BlockPos pos) {
        return new AABB(pos).inflate(2.0);
    }

    private static int xp(final GameTestHelper helper, final BlockPos pos) {
        // Native award() merges same-denomination orbs by increasing Count, not Value.
        return helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, area(pos)).stream().mapToInt(orb -> {
            final var data = new net.minecraft.nbt.CompoundTag();
            orb.addAdditionalSaveData(data);
            return orb.getValue() * data.getInt("Count");
        }).sum();
    }
}
