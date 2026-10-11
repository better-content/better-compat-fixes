package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.prettypipes.*;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.network.PipeItem;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pressurizer.PressurizerBlockEntity;
import de.ellpeck.prettypipes.pressurizer.PressurizerContainer;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import me.desht.pneumaticcraft.common.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class PrettyPipesPneumaticGameTests {
    private static PressurizerBlockEntity place(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, Registry.pressurizerBlock.defaultBlockState());
        PressurizerBlockEntity tile = (PressurizerBlockEntity) helper.getBlockEntity(relative);
        helper.assertTrue(tile instanceof PneumaticPressurizer, "Pinned native pressurizer mixin must apply");
        return tile;
    }
    private static IAirHandlerMachine air(PressurizerBlockEntity tile) {
        return ((PneumaticPressurizer) tile).bc$getAirHandler();
    }
    private static void setAir(IAirHandlerMachine air, int amount) {
        air.addAir(amount - air.getAir());
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void wholeStackPaymentAndSimulationAreAtomic(GameTestHelper helper) {
        var tile = place(helper, new BlockPos(3, 3, 3));
        var air = air(tile);
        helper.assertTrue(air.getVolume() == 1000 && air.getDangerPressure() == 5 && air.getCriticalPressure() == 7,
                "Must use actual PNCR tier-one volume and safety thresholds");
        var stack = new ItemStack(Items.IRON_INGOT, 64);
        for (int amount : new int[] {0, 1999, 2000, 2639}) {
            setAir(air, amount);
            helper.assertTrue(!tile.pressurizeItem(stack, true), "Cannot promise an unaffordable whole stack");
            helper.assertTrue(!tile.pressurizeItem(stack, false), "Cannot partially charge a stack");
            helper.assertTrue(air.getAir() == amount, "Rejected payment must leave all air untouched");
        }
        setAir(air, 2640);
        for (int i = 0; i < 4; i++) helper.assertTrue(tile.pressurizeItem(stack, true), "Simulation should succeed");
        helper.assertTrue(air.getAir() == 2640, "Repeated simulation must not charge");
        helper.assertTrue(tile.pressurizeItem(stack, false) && air.getAir() == 2000, "64 items cost exactly 640 mL once");
        helper.assertTrue(!tile.pressurizeItem(stack, false) && air.getAir() == 2000, "No second unaffordable debit");
        helper.assertTrue(!tile.pressurizeItem(ItemStack.EMPTY, false), "Empty cargo has no acceleration payment");
        setAir(air, 2010);
        helper.assertTrue(tile.pressurizeItem(new ItemStack(Items.IRON_INGOT), false) && air.getAir() == 2000,
                "One item costs ten mL and may leave exactly two bar");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void nativeTubesConnectAndEqualizeOnAllFaces(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 3, 3);
        var tile = place(helper, relative);
        var machine = air(tile);
        int totalVolume = machine.getVolume();
        for (Direction dir : Direction.values()) {
            helper.assertTrue(tile.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY, dir)
                    .orElseThrow(() -> new IllegalStateException("Missing machine air capability")) == machine,
                    "Machine capability on " + dir);
            helper.assertTrue(tile.getCapability(PNCCapabilities.AIR_HANDLER_CAPABILITY, dir).isPresent(), "Generic air capability");
            helper.assertTrue(!tile.getCapability(ForgeCapabilities.ENERGY, dir).isPresent(), "FE input must be absent");
            helper.assertTrue(tile.getCapability(Registry.pipeConnectableCapability, dir).isPresent(), "Keep native pipe connection");
            helper.setBlock(relative.relative(dir), ModBlocks.PRESSURE_TUBE.get().defaultBlockState());
            var tube = helper.getBlockEntity(relative.relative(dir)).getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY,
                    dir.getOpposite()).orElseThrow(() -> new IllegalStateException("Missing tube air capability"));
            totalVolume += tube.getVolume();
        }
        helper.assertTrue(!tile.getCapability(ForgeCapabilities.ENERGY, null).isPresent(), "No unsided FE fallback");
        setAir(machine, 4000);
        PressurizerBlockEntity.tick(helper.getLevel(), tile.getBlockPos(), tile.getBlockState(), tile);
        int totalAir = machine.getAir();
        for (Direction dir : Direction.values()) {
            var tube = helper.getBlockEntity(relative.relative(dir)).getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY,
                    dir.getOpposite()).orElseThrow(() -> new IllegalStateException("Missing tube air capability"));
            helper.assertTrue(tube.getAir() > 0, "Native transfer through " + dir);
            totalAir += tube.getAir();
        }
        helper.assertTrue(totalAir == 4000, "Native equalization conserves finite air");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void persistenceCapabilitiesAndLegacyFeMigration(GameTestHelper helper) {
        var tile = place(helper, new BlockPos(3, 3, 3));
        setAir(air(tile), 3300);
        var saved = tile.saveWithFullMetadata();
        helper.assertTrue(!saved.contains("energy"), "Do not retain electrical charge in new saves");
        var restored = (PressurizerBlockEntity) BlockEntity.loadStatic(tile.getBlockPos(), tile.getBlockState(), saved);
        helper.assertTrue(restored != null && air(restored).getAir() == 3300, "Native air NBT survives reload");
        var cap = tile.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY);
        tile.invalidateCaps();
        helper.assertTrue(!cap.isPresent(), "Invalidate old air capability");
        tile.reviveCaps();
        helper.assertTrue(tile.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY).isPresent(), "Revive air capability");
        helper.assertTrue(tile.getCapability(Registry.pipeConnectableCapability).isPresent(), "Revive native pipe capability");
        saved.remove("better_compat_fixes:pressurizer_air");
        saved.putInt("energy", 64000);
        tile.load(saved);
        helper.assertTrue(air(tile).getAir() == 0 && tile.getEnergy() == 0, "Legacy FE is discarded, never converted");
        tile.setRemoved();
        helper.assertTrue(!tile.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY).isPresent(), "Removed machine supplies no air");
        helper.getLevel().removeBlock(tile.getBlockPos(), false);
        var replacement = place(helper, new BlockPos(3, 3, 3));
        helper.assertTrue(air(replacement).getAir() == 0, "Breaking/replacing does not copy a chamber");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void noIdleDrainAndNativeOverpressure(GameTestHelper helper) {
        var tile = place(helper, new BlockPos(3, 3, 3));
        setAir(air(tile), 5000);
        for (int i = 0; i < 10; i++) PressurizerBlockEntity.tick(helper.getLevel(), tile.getBlockPos(), tile.getBlockState(), tile);
        helper.assertTrue(air(tile).getAir() == 5000, "No invented idle air drain below critical pressure");
        helper.runAfterDelay(25, () -> setAir(air(tile), 8000));
        // Add pressure after the native startup grace period; let the actual handler tick.
        helper.succeedWhen(() -> helper.assertTrue(tile.isRemoved()
                        || helper.getLevel().getBlockEntity(tile.getBlockPos()) != tile,
                "Native PNCR overpressure must destroy the overcharged machine"));
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void ordinaryNativeRoutingStillDeliversWithoutAir(GameTestHelper helper) {
        var pressurizer = place(helper, new BlockPos(2, 2, 1));
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.CHEST.defaultBlockState());
        helper.setBlock(new BlockPos(2, 1, 1), Registry.pipeBlock.defaultBlockState());
        helper.setBlock(new BlockPos(3, 1, 1), Registry.pipeBlock.defaultBlockState());
        helper.setBlock(new BlockPos(4, 1, 1), Blocks.CHEST.defaultBlockState());
        var start = (PipeBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 1));
        var end = (ChestBlockEntity) helper.getBlockEntity(new BlockPos(4, 1, 1));
        start.pressurizer = pressurizer;
        helper.assertTrue(start.getItemSpeed(new ItemStack(Items.IRON_INGOT, 64)) == 0.05F,
                "Empty air chamber must preserve native ordinary speed");
        helper.runAfterDelay(20, () -> {
            var stack = new ItemStack(Items.IRON_INGOT, 64);
            helper.assertTrue(PipeNetwork.get(helper.getLevel()).routeItemToLocation(start.getBlockPos(),
                    helper.absolutePos(new BlockPos(1, 1, 1)), helper.absolutePos(new BlockPos(3, 1, 1)),
                    end.getBlockPos(), stack, speed -> new PipeItem(stack, speed)), "Native route must remain available");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(end.getItem(0).getCount() == 64, "All physical cargo must arrive in the destination chest");
            helper.assertTrue(air(pressurizer).getAir() == 0, "Ordinary delivery must not create air");
        });
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void menuSynchronizationPreservesWholeIntegersAndViewers(GameTestHelper helper) {
        var tile = place(helper, new BlockPos(3, 3, 3));
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var first = new PressurizerContainer(Registry.pressurizerContainer, 1, player, tile.getBlockPos());
        var second = new PressurizerContainer(Registry.pressurizerContainer, 2, player, tile.getBlockPos());
        int[] firstData = new int[2];
        int[] secondData = new int[2];
        first.addSlotListener(listener(firstData));
        second.addSlotListener(listener(secondData));
        for (int value : new int[] {0, 2000, 32768, 70000, -1000}) {
            setAir(air(tile), value);
            first.broadcastChanges();
            second.broadcastChanges();
            helper.assertTrue(PressurizerAirPolicy.combine(firstData[0], firstData[1]) == value, "Full signed air snapshot");
            helper.assertTrue(PressurizerAirPolicy.combine(secondData[0], secondData[1]) == value, "Independent viewer snapshot");
        }
        helper.assertTrue(first.slots.size() == 36 && second.slots.size() == 36, "Native player slots stay unchanged");
        setAir(air(tile), 0); // High values test the wire width only, not overpressure bypass.
        helper.succeed();
    }

    private static ContainerListener listener(int[] data) {
        return new ContainerListener() {
            @Override public void slotChanged(AbstractContainerMenu menu, int slot, ItemStack stack) {}
            @Override public void dataChanged(AbstractContainerMenu menu, int index, int value) { data[index] = (short) value; }
        };
    }
}
