package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class OffhandBlockPlacementGameTests {
    private OffhandBlockPlacementGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void offhandBlocksCannotPlaceOrConsumeButMainHandCan(final GameTestHelper helper) {
        final var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "offhand_blocks"));
        player.setGameMode(GameType.SURVIVAL);
        final BlockPos support = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(support.above(), Blocks.AIR.defaultBlockState());
        final var hit = new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false);
        for (final var item : new net.minecraft.world.item.Item[]{Items.COBBLESTONE, Items.TORCH}) {
            final var stack = new ItemStack(item, 3);
            player.setItemInHand(InteractionHand.OFF_HAND, stack);
            player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.OFF_HAND, hit);
            helper.assertTrue(helper.getLevel().getBlockState(support.above()).isAir(), "Offhand block placed: " + item);
            helper.assertTrue(stack.getCount() == 3, "Denied offhand placement consumed an item");
        }
        final var mainStack = new ItemStack(Items.COBBLESTONE, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainStack);
        player.gameMode.useItemOn(player, helper.getLevel(), mainStack, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(helper.getLevel().getBlockState(support.above()).is(Blocks.COBBLESTONE), "Main-hand placement was blocked");
        helper.assertTrue(mainStack.getCount() == 2, "Main-hand placement did not consume normally");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void decorativeFlowersPlaceFromBothHandsWithNativeSupportAndQuantity(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "flower_hands"));
        player.setGameMode(GameType.SURVIVAL);
        final BlockPos support = helper.absolutePos(new BlockPos(2, 2, 2));
        final var hit = new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false);
        for (final var hand : InteractionHand.values()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            level.setBlockAndUpdate(support, Blocks.DIRT.defaultBlockState());
            level.setBlockAndUpdate(support.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(support.above(2), Blocks.AIR.defaultBlockState());
            final var flower = new ItemStack(Items.DANDELION, 2);
            player.setItemInHand(hand, flower);
            player.gameMode.useItemOn(player, level, flower, hand, hit);
            helper.assertTrue(level.getBlockState(support.above()).is(Blocks.DANDELION), "Flower denied from " + hand);
            helper.assertTrue(flower.getCount() == 1, "Native flower consumption changed: " + hand);

            level.setBlockAndUpdate(support.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
            player.gameMode.useItemOn(player, level, flower, hand, hit);
            helper.assertTrue(level.getBlockState(support.above()).isAir() && flower.getCount() == 1,
                "Invalid flower support placed or consumed: " + hand);

            level.setBlockAndUpdate(support, Blocks.DIRT.defaultBlockState());
            final var petals = new ItemStack(Items.PINK_PETALS, 3);
            player.setItemInHand(hand, petals);
            player.gameMode.useItemOn(player, level, petals, hand, hit);
            player.gameMode.useItemOn(player, level, petals, hand, hit);
            helper.assertTrue(level.getBlockState(support.above()).is(Blocks.PINK_PETALS)
                && level.getBlockState(support.above()).getValue(net.minecraft.world.level.block.PinkPetalsBlock.AMOUNT) == 2,
                "Native petal quantity changed: " + hand);
            helper.assertTrue(petals.getCount() == 1, "Petal additions must consume one per unit: " + hand);

            level.setBlockAndUpdate(support.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(support.above(2), Blocks.STONE.defaultBlockState());
            final var tall = new ItemStack(Items.SUNFLOWER, 2);
            player.setItemInHand(hand, tall);
            player.gameMode.useItemOn(player, level, tall, hand, hit);
            helper.assertTrue(level.getBlockState(support.above()).isAir() && tall.getCount() == 2,
                "Tall flower ignored blocked upper clearance: " + hand);
            level.setBlockAndUpdate(support.above(2), Blocks.AIR.defaultBlockState());
            player.gameMode.useItemOn(player, level, tall, hand, hit);
            helper.assertTrue(level.getBlockState(support.above()).is(Blocks.SUNFLOWER)
                && level.getBlockState(support.above(2)).is(Blocks.SUNFLOWER) && tall.getCount() == 1,
                "Tall flower did not retain native paired placement: " + hand);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void flowerExceptionDoesNotAdmitNonflowerBlockItemsOrOverrideOtherDenials(final GameTestHelper helper) {
        final var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "flower_controls"));
        final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        // Azalea is in upstream flowers; wheat seeds are a crop BlockItem. Neither is a decorative flower.
        for (final var item : new net.minecraft.world.item.Item[]{Items.COBBLESTONE, Items.TORCH, Items.WHEAT_SEEDS, Items.FLOWERING_AZALEA}) {
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item, 2));
            final var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.OFF_HAND, pos, hit);
            MinecraftForge.EVENT_BUS.post(event);
            helper.assertTrue(event.getUseItem() == Event.Result.DENY, "Nonflower offhand restriction changed: " + item);
            helper.assertTrue(!event.isCanceled() && event.getUseBlock() == Event.Result.DEFAULT,
                "Nonflower denial changed clicked-block interactions");
            helper.assertTrue(player.getOffhandItem().getCount() == 2, "Admission check consumed nonflower item");
        }
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DANDELION));
        final var denied = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.OFF_HAND, pos, hit);
        denied.setUseItem(Event.Result.DENY);
        com.bettercontent.bettercompatfixes.compat.OffhandBlockPlacement.onRightClickBlock(denied);
        helper.assertTrue(denied.getUseItem() == Event.Result.DENY, "Flower exception overrode another policy denial");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void blockInteractionAndNonblockOffhandUseRemainUnchanged(final GameTestHelper helper) {
        final var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "offhand_controls"));
        final BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (final var item : new net.minecraft.world.item.Item[]{Items.COBBLESTONE, Items.SHIELD, Items.WATER_BUCKET, Items.APPLE}) {
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item));
            final var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.OFF_HAND, pos, hit);
            MinecraftForge.EVENT_BUS.post(event);
            helper.assertTrue(!event.isCanceled(), "Offhand interaction was canceled wholesale");
            helper.assertTrue(event.getUseBlock() == Event.Result.DEFAULT, "Clicked-block interaction was changed");
            helper.assertTrue(event.getUseItem() == (item == Items.COBBLESTONE ? Event.Result.DENY : Event.Result.DEFAULT),
                    "Unexpected offhand item admission: " + item);
        }
        helper.succeed();
    }
}
