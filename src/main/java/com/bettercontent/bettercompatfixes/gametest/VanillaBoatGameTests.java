package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.VanillaBoatPolicy;
import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class VanillaBoatGameTests {
    private VanillaBoatGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void vanillaBoatUsesConfiguredAccumulatedDamageThreshold(final GameTestHelper helper) {
        final Vec3 position = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        final Boat boat = new Boat(helper.getLevel(), position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(boat);

        boat.hurt(helper.getLevel().damageSources().generic(), 39.0F);
        helper.assertTrue(boat.isAlive(), "Vanilla boat must survive below the scaled threshold");
        helper.assertTrue(
                boat.getDamage() == 390.0F,
                "Boat damage accumulation must remain vanilla while only the destruction threshold changes");

        boat.hurt(helper.getLevel().damageSources().generic(), 2.0F);
        helper.assertTrue(!boat.isAlive(), "Vanilla boat must break after crossing the scaled threshold");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void destroyedVanillaBoatDropsNoVesselItem(final GameTestHelper helper) {
        final AABB bounds = new AABB(
                helper.absolutePos(new BlockPos(0, 0, 0)),
                helper.absolutePos(new BlockPos(5, 5, 5)));
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).forEach(ItemEntity::discard);
        final Vec3 position = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        final Boat boat = new Boat(helper.getLevel(), position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(boat);
        boat.hurt(helper.getLevel().damageSources().generic(), 41.0F);

        final boolean vesselDropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).stream()
                .map(ItemEntity::getItem)
                .map(ItemStack::getItem)
                .anyMatch(item -> item == Items.OAK_BOAT);
        helper.assertTrue(!vesselDropped, "Destroyed vanilla boat must not drop its boat item");
        helper.assertTrue(
                helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).stream()
                        .map(ItemEntity::getItem)
                        .anyMatch(stack -> stack.is(Items.OAK_PLANKS) && stack.getCount() == 3),
                "Destroyed vanilla boat must return partial wood components");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void chestBoatSharesPolicyAndPreservesContents(final GameTestHelper helper) {
        final AABB bounds = new AABB(
                helper.absolutePos(new BlockPos(0, 0, 0)),
                helper.absolutePos(new BlockPos(5, 5, 5)));
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).forEach(ItemEntity::discard);
        final Vec3 position = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        final ChestBoat boat = new ChestBoat(helper.getLevel(), position.x, position.y, position.z);
        boat.setItem(0, new ItemStack(Items.DIAMOND));
        helper.getLevel().addFreshEntity(boat);
        boat.hurt(helper.getLevel().damageSources().generic(), 41.0F);

        final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).stream()
                .map(ItemEntity::getItem)
                .toList();
        helper.assertTrue(
                drops.stream().anyMatch(stack -> stack.is(Items.DIAMOND)),
                "Chest boat contents must still drop");
        helper.assertTrue(
                drops.stream().noneMatch(stack -> stack.is(Items.OAK_CHEST_BOAT)),
                "Destroyed chest boat must not drop its vessel item");
        helper.assertTrue(
                drops.stream().anyMatch(stack -> stack.is(Items.OAK_PLANKS) && stack.getCount() == 3)
                        && drops.stream().anyMatch(stack -> stack.is(Items.CHEST)),
                "Destroyed chest boat must return partial wood and chest components");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void policyIsLimitedToVanillaBoatEntityTypes(final GameTestHelper helper) {
        helper.assertTrue(VanillaBoatPolicy.appliesTo(EntityType.BOAT), "Vanilla boat type must be targeted");
        helper.assertTrue(VanillaBoatPolicy.appliesTo(EntityType.CHEST_BOAT), "Vanilla chest boat type must be targeted");
        helper.assertTrue(!VanillaBoatPolicy.appliesTo(EntityType.MINECART), "Unrelated and modded entity types must be excluded");
        helper.assertTrue(BcFixesConfig.vanillaBoatDurabilityMultiplier() == 10.0D, "Default multiplier must be 10.0");
        helper.succeed();
    }
}
