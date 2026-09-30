package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.WaterWheelBiomePolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class WaterWheelBiomePolicyGameTests {
    private WaterWheelBiomePolicyGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void riverTagIsTheOnlyGenerationAuthority(final GameTestHelper helper) {
        final var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        helper.assertTrue(
                biomes.getHolderOrThrow(Biomes.RIVER).is(net.minecraft.tags.BiomeTags.IS_RIVER),
                "The ordinary river biome must allow water-wheel generation");
        helper.assertTrue(
                biomes.getHolderOrThrow(Biomes.PLAINS).is(net.minecraft.tags.BiomeTags.IS_RIVER) == false,
                "Non-river biomes must not allow water-wheel generation");
        helper.assertTrue(
                WaterWheelBiomePolicy.allowsGeneration(helper.getLevel(), helper.absolutePos(new BlockPos(0, 2, 0)))
                        == helper.getLevel().getBiome(helper.absolutePos(new BlockPos(0, 2, 0))).is(net.minecraft.tags.BiomeTags.IS_RIVER),
                "Runtime policy must be exactly the river biome-tag decision");
        helper.succeed();
    }
}
