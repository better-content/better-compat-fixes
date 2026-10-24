package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.deepvoid.FleshTemplateBounds;
import com.bettercontent.bettercompatfixes.compat.deepvoid.FleshTemplateMetadata;
import java.util.ArrayList;
import java.util.Optional;
import net.mcreator.thedeepvoid.world.features.StructureFeature;
import net.mcreator.thedeepvoid.world.features.configurations.StructureFeatureConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual upstream feature and native region/template writes at a deliberately unsafe origin. */
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DeepVoidBonePlacementGameTests {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(DeepVoidBonePlacementGameTests.class);
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty",
            batch = "deep_void_bone_placement", timeoutTicks = 200)
    public static void brokenBonesFitWholeTemplatesInsideNativeWriteRegion(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        var chunks = new ArrayList<ChunkAccess>();
        for (int z = center.z - 1; z <= center.z + 1; z++)
            for (int x = center.x - 1; x <= center.x + 1; x++) chunks.add(level.getChunk(x, z));
        var region = new WorldGenRegion(level, chunks, ChunkStatus.FEATURES, 1);
        helper.assertTrue(region.getCenter().equals(center), "Actual region center must match the test envelope");
        String[] names = {"broken_bone1", "broken_bone2", "broken_bone3new"};
        for (int i = 0; i < names.length; i++) {
            var id = new ResourceLocation("the_deep_void", names[i]);
            var template = level.getStructureManager().getOrCreate(id);
            var metadata = (FleshTemplateMetadata.Template) template;
            helper.assertTrue(metadata.better_compat_fixes$palettes().size() == 1,
                    "Pinned bone template must have one actual palette: " + id);
            var origin = new BlockPos(center.getMinBlockX() - 32, 160 + i * 24, center.getMinBlockZ());
            var bounds = template.getBoundingBox(new StructurePlaceSettings(), origin);
            var shift = FleshTemplateBounds.fit(bounds.minX(), bounds.maxX(), bounds.minZ(), bounds.maxZ(),
                    center.x, center.z, 1);
            helper.assertTrue(!shift.unchanged(), "Hostile origin must require a producer-coordinate repair");
            var config = new StructureFeatureConfiguration(id, false, false,
                    HolderSet.direct(Blocks.STRUCTURE_BLOCK.builtInRegistryHolder()), Vec3i.ZERO);
            var context = new FeaturePlaceContext<>(Optional.empty(), region, level.getChunkSource().getGenerator(),
                    RandomSource.create(42L), origin, config);
            var feature = new StructureFeature(StructureFeatureConfiguration.CODEC);
            helper.assertTrue(feature.place(context), "Actual upstream feature must complete: " + id);
            int observed = 0;
            for (var block : metadata.better_compat_fixes$palettes().get(0).blocks()) {
                var expected = block.state();
                var target = block.pos().offset(origin).offset(shift.x(), 0, shift.z());
                helper.assertTrue(region.ensureCanWrite(target), "Translated payload escaped native write envelope");
                helper.assertTrue(level.getBlockState(target).equals(expected),
                        "Whole native bone payload was clipped/moved incorrectly: " + id + " at " + target);
                observed++;
            }
            helper.assertTrue(observed > 0, "Native placement must inspect a nonempty payload");
        }
        helper.succeed();
    }
}
