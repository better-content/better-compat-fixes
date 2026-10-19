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
