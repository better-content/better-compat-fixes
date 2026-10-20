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
    private static final ThreadLocal<NativeReads> NATIVE_READS = new ThreadLocal<>();

    /** Called only by the nonshipping native-fixture Mixin at the actual config getter return. */
    public static void observeNativeMultiplier(int value) {
        var reads = NATIVE_READS.get();
        if (reads != null) reads.record(value);
    }

    private static final class NativeReads {
        private final int expected;
        private final int[] values = new int[16];
        private int count;
        NativeReads(int expected) { this.expected = expected; }
        void record(int value) { if (count < values.length) values[count] = value; count++; }
        void verify() {
            if (count == 0 || count > values.length) throw new IllegalStateException("Missing/overflow native multiplier observations: " + count);
            for (int i = 0; i < count; i++) if (values[i] != expected)
                throw new IllegalStateException("Native calculation multiplier expected=" + expected + " actual=" + values[i]);
        }
        @Override public String toString() { return java.util.Arrays.toString(java.util.Arrays.copyOf(values, Math.min(count, values.length))); }
    }

    private static void observationControls(GameTestHelper helper) {
        var correct = new NativeReads(1); correct.record(1); correct.verify();
        var wrong = new NativeReads(1); wrong.record(10);
        for (var invalid : new NativeReads[]{wrong, new NativeReads(1)}) {
            boolean rejected = false;
            try { invalid.verify(); } catch (IllegalStateException expected) { rejected = true; }
            helper.assertTrue(rejected, "Native multiplier mismatch/missing-sample control was accepted");
        }
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "better_compat_fixes_felling_wear")
    public static void ordinaryAndTinkersNativeChargesScaleWithoutAnInventedFloor(GameTestHelper helper) {
        int originalMultiplier = BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.get();
        var originalMode = DTConfigs.AXE_DAMAGE_MODE.get();
        try (var common = isolatedConfig(BcFixesConfig.SPEC);
             var trees = isolatedConfig(DTConfigs.SERVER_CONFIG)) {
            observationControls(helper);
            DTConfigs.AXE_DAMAGE_MODE.set(DynamicTrees.AxeDamage.THICKNESS);
            var branch = oakBranch();
            var player = player(helper);
            for (int radius : new int[]{1, 2, 3, 4, 7, 8}) {
                for (int multiplier : new int[]{1, 10}) {
                    BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.set(multiplier);
                    ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
                    int actual = cut(helper, player, branch, radius, axe, true, multiplier);
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
                        int loss = cut(helper, player, branch, radius, tinkers, false, multiplier);
                        helper.assertTrue(loss == multiplier * extra, "TCon additional charge: " + name + " r=" + radius
                                + " multiplier=" + multiplier + " expected=" + (multiplier * extra) + " actual=" + loss);
                    }
                }
            }
            // Leave deliberately different temporary values: a no-op restore must fail below.
            BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.set(originalMultiplier == 1 ? 10 : 1);
            DTConfigs.AXE_DAMAGE_MODE.set(java.util.Arrays.stream(DynamicTrees.AxeDamage.values())
                    .filter(value -> value != originalMode).findFirst().orElseThrow());
        }
        helper.assertTrue(BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.get() == originalMultiplier,
                "Original effective owner multiplier not restored");
        helper.assertTrue(DTConfigs.AXE_DAMAGE_MODE.get() == originalMode, "Original effective native DT mode not restored");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "better_compat_fixes_felling_wear")
    public static void unrelatedChargesAndNativeImmunityRemainUnchanged(GameTestHelper helper) {
        int originalMultiplier = BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.get();
        try (var common = isolatedConfig(BcFixesConfig.SPEC)) {
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
            cut(helper, player, branch, 8, nearBroken, true, 10);
            helper.assertTrue(nearBroken.isEmpty(), "Native tool break must not become an admission check");
            var unbreakable = new ItemStack(Items.DIAMOND_AXE);
            unbreakable.getOrCreateTag().putBoolean("Unbreakable", true);
            helper.assertTrue(cut(helper, player, oakBranch(), 8, unbreakable, true, 10) == 0, "Unbreakable immunity changed");
            // Unjoined native player: exercise the actual ItemStack creative immunity flag
            // without invoking the login-dependent game-mode packet path.
            player.getAbilities().instabuild = true;
            helper.assertTrue(cut(helper, player, oakBranch(), 8, new ItemStack(Items.DIAMOND_AXE), true, 10) == 0,
                    "Creative immunity changed");
            BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.set(originalMultiplier == 1 ? 10 : 1);
        }
        helper.assertTrue(BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.get() == originalMultiplier,
                "Original effective multiplier not restored after immunity controls");
        helper.succeed();
    }

    /** Avoid autosave/reload races while varying native config; never edit the fixture's file config. */
    private static ConfigScope isolatedConfig(net.minecraftforge.common.ForgeConfigSpec spec) {
        var original = net.minecraftforge.fml.config.ConfigTracker.INSTANCE.configSets().values().stream()
                .flatMap(java.util.Collection::stream).filter(config -> config.getSpec() == spec)
                .findFirst().orElseThrow().getConfigData();
        var originalValues = copyConfig(original);
        if (!(original instanceof com.electronwill.nightconfig.core.file.FileConfig file))
            throw new IllegalStateException("Expected actual native file config for restoration control");
        var path = file.getNioPath();
        byte[] originalBytes = configBytes(path);
        spec.setConfig(copyConfig(original));
        return () -> {
            spec.setConfig(original);
            if (!originalValues.valueMap().equals(original.valueMap()))
                throw new IllegalStateException("Fixture mutated original native config values");
            if (!java.util.Arrays.equals(originalBytes, configBytes(path)))
                throw new IllegalStateException("Fixture changed original native config file bytes");
        };
    }

    private static byte[] configBytes(java.nio.file.Path path) {
        try { return java.nio.file.Files.readAllBytes(path); }
        catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }

    private static com.electronwill.nightconfig.core.CommentedConfig copyConfig(
            com.electronwill.nightconfig.core.UnmodifiableConfig source) {
        var copy = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        for (var entry : source.entrySet()) {
            Object value = entry.getValue();
            copy.set(java.util.List.of(entry.getKey()),
                    value instanceof com.electronwill.nightconfig.core.UnmodifiableConfig section
                            ? copyConfig(section) : value);
        }
        return copy;
    }

    @FunctionalInterface
    private interface ConfigScope extends AutoCloseable {
        @Override void close();
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
                           int radius, ItemStack stack, boolean ordinaryMining, int expectedMultiplier) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var state = branch.getStateForRadius(radius);
        helper.getLevel().setBlockAndUpdate(pos, state);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        int before = stack.getDamageValue();
        var observed = new NativeReads(expectedMultiplier);
        if (NATIVE_READS.get() != null) throw new IllegalStateException("Nested native multiplier observation");
        NATIVE_READS.set(observed);
        try {
            if (ordinaryMining) stack.mineBlock(helper.getLevel(), state, pos, player);
            int afterMining = stack.getDamageValue();
            var worldBeforeFall = helper.getLevel().getBlockState(pos);
            branch.futureBreak(state, helper.getLevel(), pos, player);
            com.mojang.logging.LogUtils.getLogger().info(
                    "DT wear fixture r={} nativeRadius={} worldRadius={} multiplier={} item={} mining={} initial={} deferred={} branch={} nativeMultipliers={}",
                    radius, branch.getRadius(state), branch.getRadius(worldBeforeFall),
                    BcFixesConfig.DYNAMIC_TREES_FELLING_DURABILITY_MULTIPLIER.get(), stack.getItem(),
                    ordinaryMining, afterMining - before, stack.getDamageValue() - afterMining, branch.getClass().getName(), observed);
            observed.verify();
            return stack.getDamageValue() - before;
        } finally { NATIVE_READS.remove(); }
    }
}
