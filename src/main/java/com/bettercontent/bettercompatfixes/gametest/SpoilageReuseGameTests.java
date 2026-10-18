package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.StarcatcherSpoilage;
import com.wdiscute.starcatcher.bob.FishingBobEntity;
import com.wdiscute.starcatcher.io.ModDataComponents;
import com.wdiscute.starcatcher.io.SingleStackContainer;
import com.wdiscute.starcatcher.rod.FishingRodMenu;
import com.wdiscute.starcatcher.storage.FishProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Native bridge fixture, not a claim that the full pack/Industrial Heat was loaded. */
@PrefixGameTestTemplate(false)
public final class SpoilageReuseGameTests {
    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void produceBridgeConsumesAndProducesNativeFertilizer(GameTestHelper helper) {
        var id = new ResourceLocation("better_industrial_heat", "spoiled_produce");
        var item = ForgeRegistries.ITEMS.containsKey(id) ? ForgeRegistries.ITEMS.getValue(id)
                : net.minecraft.world.item.Items.POISONOUS_POTATO;
        var map = net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES;
        boolean had = map.containsKey(item);
        float previous = map.getFloat(item);
        com.bettercontent.bettercompatfixes.compat.SpoilageReuse.registerProduce(item);
        try {
            var pos = helper.absolutePos(new BlockPos(2, 1, 2));
            var state = Blocks.COMPOSTER.defaultBlockState();
            helper.getLevel().setBlockAndUpdate(pos, state);
            var stack = new ItemStack(item, 2);
            var filled = net.minecraft.world.level.block.ComposterBlock.insertItem(null, state, helper.getLevel(), stack, pos);
            helper.assertTrue(stack.getCount() == 1 && filled.getValue(net.minecraft.world.level.block.ComposterBlock.LEVEL) == 1,
                    "Native first compost layer must consume produce and advance the composter");
            var ready = state.setValue(net.minecraft.world.level.block.ComposterBlock.LEVEL, 8);
            helper.getLevel().setBlockAndUpdate(pos, ready);
            net.minecraft.world.level.block.ComposterBlock.extractProduce(null, ready, helper.getLevel(), pos);
            var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(1));
            helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(net.minecraft.world.item.Items.BONE_MEAL)),
                    "Completed native compost must return usable bone meal fertilizer");
        } finally {
            if (had) map.put(item, previous); else map.removeFloat(item);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void basicBaitBridgeLoadsBenefitsAndConsumesInNativeRod(GameTestHelper helper) throws Exception {
        var disabledFillet = helper.getLevel().getRecipeManager().byKey(new ResourceLocation("starcatcher", "aquaculture_fillet"));
        helper.assertTrue(disabledFillet.isPresent() && disabledFillet.get().getResultItem(helper.getLevel().registryAccess())
                        .is(net.minecraft.world.item.Items.STICK),
                "Native recipe loading must use the exact authored disabled override, not the malformed optional recipe");
        // Use the actual production meat when that optional provider is present; otherwise an explicit native wither-skull bait fixture.
        var meatId = new ResourceLocation("better_industrial_heat", "spoiled_meat");
        var baitItem = ForgeRegistries.ITEMS.getValue(ForgeRegistries.ITEMS.containsKey(meatId)
                ? meatId : new ResourceLocation("minecraft", "wither_skeleton_skull"));
        var previousDefaults = ModDataComponents.getDefaults().get(baitItem);
        StarcatcherSpoilage.registerBasicBait(baitItem);
        var player = FakePlayerFactory.get(helper.getLevel(), new com.mojang.authlib.GameProfile(
                java.util.UUID.fromString("7fc91369-142e-43d9-b74d-e563aad8793e"), "SpoilageBridge"));
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        var rod = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("starcatcher", "humble_rod")));
        player.setItemInHand(InteractionHand.MAIN_HAND, rod);
        var menu = new FishingRodMenu(0, player.getInventory(), rod);
        var bait = new ItemStack(baitItem, 3);
        helper.assertTrue(menu.getSlot(37).mayPlace(bait), "Native bait slot must admit the registered bait");
        menu.getSlot(37).set(bait);
        menu.removed(player);
        helper.assertTrue(ModDataComponents.get(rod, ModDataComponents.BAIT).stack().getCount() == 3,
                "Native menu must persist loaded bait on the rod");
        var emptyRod = rod.copy();
        ModDataComponents.set(emptyRod, ModDataComponents.BAIT, new SingleStackContainer(ItemStack.EMPTY));
        var emptyBob = new FishingBobEntity(helper.getLevel(), player, emptyRod);
        var bob = new FishingBobEntity(helper.getLevel(), player, rod);
        try {
            helper.assertTrue(bob.minTicksToFish < emptyBob.minTicksToFish && bob.maxTicksToFish < emptyBob.maxTicksToFish,
                    "Native bobber must apply the basic lure-time benefit, not merely accept a tag");
            helper.assertTrue(!FishProperties.BaitRestrictions.LEGENDARY_BAIT.correctBait().contains(meatId)
                            && !FishProperties.BaitRestrictions.LUSH_BAIT.correctBait().contains(meatId),
                    "Spoiled meat must not satisfy specialist or legendary bait restrictions");
            helper.setBlock(new BlockPos(2, 1, 2), Blocks.WATER);
            helper.setBlock(new BlockPos(2, 2, 2), Blocks.WATER);
            bob.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
            bob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            // Preserve the benefit assertions above; only accelerate this fixture's PUBLIC timing fields.
            // nextInt(1) is always zero, so the native max-time fallback deterministically triggers the bite.
            bob.minTicksToFish = 0;
            bob.maxTicksToFish = 0;
            bob.ticksInFluid = 0;
            bob.chanceToFishEachTick = 1;
            bob.tick(); // Native fluid contact moves FLYING -> BOBBING.
            helper.assertTrue(bob.ticksInFluid == 0,
                    "Initial native fluid contact must precede the bobbing timer");
            bob.tick(); // Native bobbing timer moves BOBBING -> BITING.
            helper.assertTrue(bob.getEntityData().get(FishingBobEntity.STATE) == 2 && bob.ticksInFluid == 1,
                    "Native tick must observe BOBBING and advance its timer before entering the bite");
            helper.assertTrue(ModDataComponents.get(rod, ModDataComponents.BAIT).stack().getCount() == 3,
                    "Native contact and bite onset must not consume bait early");
            bob.timeBiting = 80;
            bob.tick(); // Native missed-bite timeout consumes exactly one bait.
            helper.assertTrue(bob.getEntityData().get(FishingBobEntity.STATE) == 3 && bob.timeBiting == 81,
                    "Native BITING state must advance through the public missed-bite timeout");
            helper.assertTrue(ModDataComponents.get(rod, ModDataComponents.BAIT).stack().getCount() == 2,
                    "Native missed bite must consume exactly one bait");
        } finally {
            bob.discard();
            emptyBob.discard();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (previousDefaults == null) ModDataComponents.getDefaults().remove(baitItem);
            else ModDataComponents.getDefaults().put(baitItem, previousDefaults);
        }
        helper.succeed();
    }
}
