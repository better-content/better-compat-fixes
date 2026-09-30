package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

@PrefixGameTestTemplate(false)
public final class SophisticatedBarrelHopperGameTests {
    private static final ResourceLocation BARREL = new ResourceLocation("sophisticatedstorage", "barrel");
    private static final ResourceLocation LIMITED_BARREL =
            new ResourceLocation("sophisticatedstorage", "limited_barrel_1");

    private SophisticatedBarrelHopperGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void vanillaHopperExtractsFromBarrel(final GameTestHelper helper) {
        assertTransfersFrom(helper, BARREL);
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void vanillaHopperExtractsFromLimitedBarrel(final GameTestHelper helper) {
        assertTransfersFrom(helper, LIMITED_BARREL);
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void fullVanillaHopperDoesNotConsumeBarrelItem(final GameTestHelper helper) {
        final TestInventory inventory = placeBarrelAndHopper(helper, BARREL);
        for (int slot = 0; slot < inventory.hopper().getContainerSize(); slot++) {
            inventory.hopper().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        inventory.source().insertItem(0, new ItemStack(Items.DIRT), false);

        helper.runAfterDelay(12, () -> {
            final ItemStack remaining = inventory.source().getStackInSlot(0);
            if (!remaining.is(Items.DIRT) || remaining.getCount() != 1) {
                helper.fail("Expected a full hopper to leave the Sophisticated barrel unchanged");
                return;
            }
            helper.succeed();
        });
    }

    private static void assertTransfersFrom(final GameTestHelper helper, final ResourceLocation barrelId) {
        final TestInventory inventory = placeBarrelAndHopper(helper, barrelId);
        inventory.source().insertItem(0, new ItemStack(Items.DIRT), false);

        helper.runAfterDelay(12, () -> {
            if (!inventory.source().getStackInSlot(0).isEmpty()) {
                helper.fail("Expected vanilla hopper to extract the item from " + barrelId);
                return;
            }
            if (!inventory.hopper().getItem(0).is(Items.DIRT)) {
                helper.fail("Expected extracted item in vanilla hopper below " + barrelId);
                return;
            }
            helper.succeed();
        });
    }

    private static TestInventory placeBarrelAndHopper(
            final GameTestHelper helper,
            final ResourceLocation barrelId
    ) {
        final Block barrel = ForgeRegistries.BLOCKS.getValue(barrelId);
        if (barrel == null || barrel == Blocks.AIR) {
            throw new IllegalStateException("Missing required Sophisticated Storage block " + barrelId);
        }

        final BlockPos relativeHopperPos = new BlockPos(2, 2, 2);
        final BlockPos relativeBarrelPos = relativeHopperPos.above();
        helper.setBlock(relativeHopperPos, Blocks.HOPPER.defaultBlockState()
                .setValue(HopperBlock.FACING, Direction.DOWN)
                .setValue(HopperBlock.ENABLED, true));
        helper.setBlock(relativeBarrelPos, barrel.defaultBlockState());

        final BlockPos hopperPos = helper.absolutePos(relativeHopperPos);
        final BlockPos barrelPos = helper.absolutePos(relativeBarrelPos);

        final BlockEntity barrelBlockEntity = helper.getLevel().getBlockEntity(barrelPos);
        if (barrelBlockEntity == null) {
            throw new IllegalStateException("Missing block entity for " + barrelId);
        }
        final IItemHandler source = barrelBlockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                .orElseThrow(() -> new IllegalStateException("Missing item handler for " + barrelId));
        final BlockEntity hopperBlockEntity = helper.getLevel().getBlockEntity(hopperPos);
        if (!(hopperBlockEntity instanceof HopperBlockEntity hopper)) {
            throw new IllegalStateException("Missing vanilla hopper block entity");
        }
        return new TestInventory(source, hopper);
    }

    private record TestInventory(IItemHandler source, HopperBlockEntity hopper) {
    }
}
