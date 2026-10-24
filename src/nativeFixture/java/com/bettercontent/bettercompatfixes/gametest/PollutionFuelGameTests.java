package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.fuelfixture.mixin.HearthFuelAccess;
import com.bettercontent.bettercompatfixes.fuelfixture.mixin.SteamLocomotiveFuelAccess;
import com.bettercontent.bettercompatfixes.fuelfixture.mixin.SteamTugFuelAccess;
import com.bettercontent.bettercompatfixes.nativewitness.fuel.FuelEmissionWitness;
import com.endertech.minecraft.mods.adpother.events.WorldEvents;
import com.momosoftworks.coldsweat.common.blockentity.BoilerBlockEntity;
import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import com.mojang.logging.LogUtils;
import dev.murad.shipping.entity.custom.train.locomotive.SteamLocomotiveEntity;
import dev.murad.shipping.entity.custom.vessel.tug.SteamTugEntity;
import dev.murad.shipping.util.FuelItemStackHandler;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Real fuel methods and actual AdPother state transitions, not Mixin-application witnesses. */
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PollutionFuelGameTests {
    private PollutionFuelGameTests() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(PollutionFuelGameTests.class);
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "fuel_emission")
    public static void hearthAutomaticFuelEmitsExactlyOnce(GameTestHelper helper) {
        checkHearth(helper, "hearth_bottom", false);
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "fuel_emission")
    public static void boilerInheritedAutomaticFuelEmitsExactlyOnce(GameTestHelper helper) {
        checkHearth(helper, "boiler", true);
    }

    private static void checkHearth(GameTestHelper helper, String name, boolean boiler) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cold_sweat", name));
        helper.assertTrue(block != null && block != Blocks.AIR, "Actual ColdSweat block required: " + name);
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        var tile = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(tile instanceof HearthBlockEntity, "Actual native Hearth subtype required: " + name);
        var hearth = (HearthBlockEntity) tile;
        helper.assertTrue((hearth instanceof BoilerBlockEntity) == boiler, "Wrong upstream native block-entity type");
        var access = (HearthFuelAccess) hearth;
        access.betterContentFuelFixture$ticksExisted(0);
        helper.assertTrue(hearth.getFuelDrainInterval() > 0, "Actual native automatic fuel interval must be enabled");
        for (int repeat = 0; repeat < 2; repeat++) {
            hearth.setHotFuel(4, false);
            // Original drainHotFuel(..., true) calls onFuelChanged -> checkInputSignal,
            // which legitimately clears usage when this isolated fixture has no redstone.
            // Establish the same active-usage precondition for EACH automatic operation.
            access.betterContentFuelFixture$usingColdFuel(false);
            access.betterContentFuelFixture$usingHotFuel(true);
            var actual = FuelEmissionWitness.observe(access::betterContentFuelFixture$tickDrainFuel);
            helper.assertTrue(hearth.getHotFuel() == 3, "One actual automatic hot-fuel unit must be consumed");
            compareOneWithNativeReferences(helper, name + "-" + repeat, actual,
                    () -> WorldEvents.onLavaFuelBurned(hearth, 1));
        }
        hearth.setHotFuel(0, false);
        access.betterContentFuelFixture$usingColdFuel(false);
        access.betterContentFuelFixture$usingHotFuel(true);
        assertZero(helper, name + "-empty", FuelEmissionWitness.observe(access::betterContentFuelFixture$tickDrainFuel));
        helper.assertTrue(hearth.getHotFuel() == 0, "Empty hot fuel must remain empty");

        hearth.setHotFuel(4, false);
        hearth.setColdFuel(4, false);
        access.betterContentFuelFixture$usingHotFuel(false);
        access.betterContentFuelFixture$usingColdFuel(true);
        assertZero(helper, name + "-cold-only", FuelEmissionWitness.observe(access::betterContentFuelFixture$tickDrainFuel));
        helper.assertTrue(hearth.getHotFuel() == 4 && hearth.getColdFuel() == 3,
                "Cold-only automatic operation must consume cold fuel without hot pollution");
        // Manual extraction invokes the parameterized overload, NOT the automatic combustion hook.
        assertZero(helper, name + "-manual-extraction", FuelEmissionWitness.observe(() -> hearth.drainHotFuel(1, false)));
        helper.assertTrue(hearth.getHotFuel() == 3, "Manual extraction must still perform its original fuel mutation");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "fuel_emission")
    public static void locomotiveFuelConsumptionEmitsExactlyOnce(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var entity = new SteamLocomotiveEntity(helper.getLevel(), (double) pos.getX() + 0.5,
                (double) pos.getY(), (double) pos.getZ() + 0.5);
        var access = (SteamLocomotiveFuelAccess) entity;
        checkSteam(helper, entity, access.betterContentFuelFixture$fuelHandler(),
                access::betterContentFuelFixture$tickFuel, access::betterContentFuelFixture$burnTime,
                access::betterContentFuelFixture$burnTime);
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "fuel_emission")
    public static void tugFuelConsumptionEmitsExactlyOnce(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var entity = new SteamTugEntity(helper.getLevel(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        var access = (SteamTugFuelAccess) entity;
        checkSteam(helper, entity, access.betterContentFuelFixture$fuelHandler(),
                access::betterContentFuelFixture$tickFuel, access::betterContentFuelFixture$burnTime,
                access::betterContentFuelFixture$burnTime);
    }

    private static void checkSteam(GameTestHelper helper, Entity entity, FuelItemStackHandler fuel,
            BooleanSupplier tickFuel, IntSupplier burnTime, IntConsumer setBurnTime) {
        helper.assertTrue(helper.getLevel().addFreshEntity(entity), "Actual registered upstream entity must spawn");
        String name = entity.getType().toString();
        for (int repeat = 0; repeat < 2; repeat++) {
            setBurnTime.accept(0);
            fuel.setStackInSlot(0, new ItemStack(Items.COAL, 2));
            var actual = FuelEmissionWitness.observe(() -> helper.assertTrue(tickFuel.getAsBoolean(),
                    "Actual coal consumption must start the native burn"));
            helper.assertTrue(fuel.getStackInSlot(0).is(Items.COAL) && fuel.getStackInSlot(0).getCount() == 1,
                    "One actual inventory fuel item must be consumed, preserving the remaining item");
            helper.assertTrue(burnTime.getAsInt() > 0, "Original positive native burn time must be preserved");
            compareOneWithNativeReferences(helper, name + "-" + repeat, actual,
                    () -> WorldEvents.onFuelBurned(new ItemStack(Items.COAL), 1, entity, WorldEvents.Alignment.CENTER));
            int before = burnTime.getAsInt();
            assertZero(helper, name + "-remaining-burn", FuelEmissionWitness.observe(() ->
                    helper.assertTrue(tickFuel.getAsBoolean(), "Existing native burn must continue")));
            helper.assertTrue(burnTime.getAsInt() == before - 1 && fuel.getStackInSlot(0).getCount() == 1,
                    "Continuing burn must not consume or emit another fuel item");
        }
        setBurnTime.accept(0);
        fuel.setStackInSlot(0, ItemStack.EMPTY);
        assertZero(helper, name + "-empty", FuelEmissionWitness.observe(() ->
                helper.assertTrue(!tickFuel.getAsBoolean(), "Empty inventory must not start a burn")));
        helper.assertTrue(burnTime.getAsInt() == 0 && fuel.getStackInSlot(0).isEmpty(), "Empty operation changed state");
        fuel.setStackInSlot(0, new ItemStack(Items.STONE));
        assertZero(helper, name + "-invalid", FuelEmissionWitness.observe(() ->
                helper.assertTrue(!tickFuel.getAsBoolean(), "Nonfuel item must not start a burn")));
        helper.assertTrue(fuel.getStackInSlot(0).is(Items.STONE) && fuel.getStackInSlot(0).getCount() == 1,
                "Original invalid-fuel operation must preserve inventory");
        helper.succeed();
    }

    private static void compareOneWithNativeReferences(GameTestHelper helper, String label,
            FuelEmissionWitness.Snapshot actual, Runnable oneNativeEmission) {
        // All calls are synchronous on the server thread at the same real object/position.
        // The observed int+fraction delta is independent of prior fractional carry and gas RNG.
        var one = FuelEmissionWitness.observe(oneNativeEmission);
        helper.assertTrue(one.completed() == 1 && one.positive(),
                "Native positive reference requires configured emitter/fuel/pollutant: " + label + " " + one);
        helper.assertTrue(actual.equivalent(one), "Actual operation did not emit exactly one native dose: " + label
                + " actual=" + actual + " reference=" + one);
        var doubled = FuelEmissionWitness.observe(() -> { oneNativeEmission.run(); oneNativeEmission.run(); });
        helper.assertTrue(doubled.completed() == 2 && doubled.positive() && !actual.equivalent(doubled),
                "Observer must reject a genuine doubled native emission, not tolerate duplicate calls: " + label);
        LogUtils.getLogger().info("BC_NATIVE_FUEL {} actual={} reference={} doubled={}", label, actual, one, doubled);
    }

    private static void assertZero(GameTestHelper helper, String label, FuelEmissionWitness.Snapshot actual) {
        helper.assertTrue(actual.completed() == 0 && actual.amounts().isEmpty(),
                "Noncombustion operation emitted pollution: " + label + " " + actual);
        LogUtils.getLogger().info("BC_NATIVE_FUEL {} zero={}", label, actual);
    }
}
