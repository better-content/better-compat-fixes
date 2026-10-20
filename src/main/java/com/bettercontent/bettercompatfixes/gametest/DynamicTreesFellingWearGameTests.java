package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.ferreusveritas.dynamictrees.api.TreeRegistry;
import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.init.DTConfigs;
import com.ferreusveritas.dynamictrees.DynamicTrees;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;

import java.util.UUID;

/** Authored native API fixtures, not human-input or full-pack gameplay acceptance. */
@PrefixGameTestTemplate(false)
public final class DynamicTreesFellingWearGameTests {
    private DynamicTreesFellingWearGameTests() {}

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void ordinaryAndTinkersNativeChargesScaleWithoutAnInventedFloor(GameTestHelper helper) {
        int previous = BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.get();
        var previousMode = DTConfigs.AXE_DAMAGE_MODE.get();
        try {
            DTConfigs.AXE_DAMAGE_MODE.set(DynamicTrees.AxeDamage.THICKNESS);
            var branch = oakBranch();
            var player = player(helper);
            for (int radius : new int[]{1, 2, 3, 4, 7, 8}) {
                for (int multiplier : new int[]{1, 10}) {
                    BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.set(multiplier);
                    ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
                    int actual = cut(helper, player, branch, radius, axe, true);
                    int extra = Math.max(0, Math.max(1, radius) / 2 - 1);
                    helper.assertTrue(actual == multiplier * (1 + extra), "Ordinary axe charge: r=" + radius
                            + " multiplier=" + multiplier + " expected=" + (multiplier * (1 + extra)) + " actual=" + actual);
                    for (String name : new String[]{"hand_axe", "broad_axe"}) {
                        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("tconstruct", name));
                        helper.assertTrue(item instanceof IModifiable, "Missing actual TCon axe " + name);
                        ItemStack tinkers = ToolBuildHandler.createSingleMaterial((IModifiable) item,
                                MaterialVariant.of(new MaterialId("tconstruct:iron"), ""));
                        helper.assertTrue(tinkers.getMaxDamage() > 100, "Native iron tool must have sufficient durability");
                        // Native TCon breakBlock skips its ordinary charge for DT's false removal result.
                        // The bytecode contract proves that path; exercise the actual deferred API here.
                        int loss = cut(helper, player, branch, radius, tinkers, false);
                        helper.assertTrue(loss == multiplier * extra, "TCon additional charge: " + name + " r=" + radius
                                + " multiplier=" + multiplier + " expected=" + (multiplier * extra) + " actual=" + loss);
                    }
                }
            }
        } finally {
            BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.set(previous);
            DTConfigs.AXE_DAMAGE_MODE.set(previousMode);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void unrelatedChargesAndNativeImmunityRemainUnchanged(GameTestHelper helper) {
        int previous = BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.get();
        try {
            BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.set(10);
            var player = player(helper);
            var pos = helper.absolutePos(new BlockPos(2, 2, 2));
            var axe = new ItemStack(Items.DIAMOND_AXE);
            player.setItemInHand(InteractionHand.MAIN_HAND, axe);
            axe.mineBlock(helper.getLevel(), net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(), pos, player);
            helper.assertTrue(axe.getDamageValue() == 1, "Ordinary log wear changed");
            axe.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            helper.assertTrue(axe.getDamageValue() == 2, "Mining context leaked to later unrelated damage");
            var branch = oakBranch();
            var stripState = branch.getStateForRadius(8);
            helper.getLevel().setBlockAndUpdate(pos, stripState);
            var stripping = new ItemStack(Items.DIAMOND_AXE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stripping);
            branch.stripBranch(stripState, helper.getLevel(), pos, player, stripping);
            helper.assertTrue(stripping.getDamageValue() == 2, "Stripping inherited the felling multiplier");
            var nearBroken = new ItemStack(Items.DIAMOND_AXE);
            nearBroken.setDamageValue(nearBroken.getMaxDamage() - 1);
            cut(helper, player, branch, 8, nearBroken, true);
            helper.assertTrue(nearBroken.isEmpty(), "Native tool break must not become an admission check");
            var unbreakable = new ItemStack(Items.DIAMOND_AXE);
            unbreakable.getOrCreateTag().putBoolean("Unbreakable", true);
            helper.assertTrue(cut(helper, player, oakBranch(), 8, unbreakable, true) == 0, "Unbreakable immunity changed");
            // Unjoined native player: exercise the actual ItemStack creative immunity flag
            // without invoking the login-dependent game-mode packet path.
            player.getAbilities().instabuild = true;
            helper.assertTrue(cut(helper, player, oakBranch(), 8, new ItemStack(Items.DIAMOND_AXE), true) == 0,
                    "Creative immunity changed");
        } finally {
            BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.set(previous);
        }
        helper.succeed();
    }

    private static BranchBlock oakBranch() {
        return TreeRegistry.findSpecies(new ResourceLocation("dynamictrees", "oak"))
                .getFamily().getBranch().orElseThrow();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "FellingWear"));
        player.getAbilities().instabuild = false;
        return player;
    }

    private static int cut(GameTestHelper helper, ServerPlayer player, BranchBlock branch,
                           int radius, ItemStack stack, boolean ordinaryMining) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var state = branch.getStateForRadius(radius);
        helper.getLevel().setBlockAndUpdate(pos, state);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        int before = stack.getDamageValue();
        if (ordinaryMining) stack.mineBlock(helper.getLevel(), state, pos, player);
        int afterMining = stack.getDamageValue();
        var worldBeforeFall = helper.getLevel().getBlockState(pos);
        branch.futureBreak(state, helper.getLevel(), pos, player);
        com.mojang.logging.LogUtils.getLogger().info(
                "DT wear fixture r={} nativeRadius={} worldRadius={} multiplier={} item={} mining={} initial={} deferred={} branch={}",
                radius, branch.getRadius(state), branch.getRadius(worldBeforeFall),
                BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.get(), stack.getItem(),
                ordinaryMining, afterMining - before, stack.getDamageValue() - afterMining, branch.getClass().getName());
        return stack.getDamageValue() - before;
    }
}
