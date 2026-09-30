package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.DecorativeVegetationTrample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class DecorativeVegetationTrampleGameTests {
    private DecorativeVegetationTrampleGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void movingEntityTramplesOnlyDecorativeVegetation(final GameTestHelper helper) {
        final BlockPos grass = new BlockPos(2, 2, 2);
        final BlockPos crop = new BlockPos(4, 2, 2);
        helper.setBlock(grass, Blocks.TALL_GRASS.defaultBlockState());
        helper.setBlock(crop, Blocks.WHEAT.defaultBlockState());

        helper.assertTrue(
                DecorativeVegetationTrample.trampleIfEntering(
                        helper.getLevel(), helper.absolutePos(grass), new Vec3(0.1D, 0.0D, 0.0D), true, 0.10D, 0.05D),
                "A successful entry roll must trample tall grass");
        helper.assertBlockPresent(Blocks.AIR, grass);
        helper.assertTrue(
                !DecorativeVegetationTrample.trampleIfEntering(
                        helper.getLevel(), helper.absolutePos(crop), new Vec3(0.1D, 0.0D, 0.0D), true, 1.0D, 0.0D),
                "Crops must not be treated as decorative vegetation");
        helper.assertBlockPresent(Blocks.WHEAT, crop);
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void stationaryEntityDoesNotTrampleVegetation(final GameTestHelper helper) {
        final BlockPos grass = new BlockPos(2, 2, 2);
        helper.setBlock(grass, Blocks.TALL_GRASS.defaultBlockState());
        helper.assertTrue(
                !DecorativeVegetationTrample.trampleIfEntering(
                        helper.getLevel(), helper.absolutePos(grass), Vec3.ZERO, true, 1.0D, 0.0D),
                "Standing still must not trample decorative vegetation");
        helper.assertBlockPresent(Blocks.TALL_GRASS, grass);
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void failedRollDoesNotRetryUntilReentry(final GameTestHelper helper) {
        final BlockPos grass = new BlockPos(2, 2, 2);
        helper.setBlock(grass, Blocks.GRASS.defaultBlockState());
        final Vec3 moving = new Vec3(0.1D, 0.0D, 0.0D);
        helper.assertTrue(
                !DecorativeVegetationTrample.trampleIfEntering(
                        helper.getLevel(), helper.absolutePos(grass), moving, true, 0.10D, 0.90D),
                "A failed probability roll must preserve grass");
        helper.assertTrue(
                !DecorativeVegetationTrample.trampleIfEntering(
                        helper.getLevel(), helper.absolutePos(grass), moving, false, 1.0D, 0.0D),
                "Remaining in the same block must not reroll");
        helper.assertTrue(
                DecorativeVegetationTrample.trampleIfEntering(
                        helper.getLevel(), helper.absolutePos(grass), moving, true, 0.10D, 0.0D),
                "Re-entering the block may roll again");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void fernsAreNotTrampled(final GameTestHelper helper) {
        final BlockPos fern = new BlockPos(2, 2, 2);
        helper.setBlock(fern, Blocks.FERN.defaultBlockState());
        helper.assertTrue(
                !DecorativeVegetationTrample.trampleIfEntering(
                        helper.getLevel(), helper.absolutePos(fern), new Vec3(0.1D, 0.0D, 0.0D), true, 1.0D, 0.0D),
                "Ferns are outside the decorative grass policy");
        helper.assertBlockPresent(Blocks.FERN, fern);
        helper.succeed();
    }
}
