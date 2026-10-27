package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import untamedwilds.entity.ComplexMob;
import untamedwilds.util.EntityDataHolder;

@PrefixGameTestTemplate(false)
public final class UntamedClientEntityDataGameTests {

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "migrated_compat")
    public static void absentWhaleSpeciesLoadsBundledDefinitionWithoutReplacingCachedData(GameTestHelper helper) {
        EntityType<?> whale = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("untamedwilds", "baleen_whale"));
        helper.assertTrue(whale != null && whale != EntityType.PIG, "Pinned whale registry fixture must exist");
        EntityDataHolder previous = ComplexMob.ENTITY_DATA_HASH.remove(whale);
        try {
            helper.assertTrue(UntamedClientEntityData.load(whale, Dist.DEDICATED_SERVER) == null,
                    "Server must not synthesize client species data");
            helper.assertTrue(!ComplexMob.ENTITY_DATA_HASH.containsKey(whale), "Server refusal must not populate the cache");
            helper.assertTrue(UntamedClientEntityData.load(EntityType.PIG, Dist.CLIENT) == null, "Foreign entity types must not receive Untamed data");
            EntityDataHolder decoded = UntamedClientEntityData.load(whale, Dist.CLIENT);
            helper.assertTrue(decoded != null && !decoded.getSpeciesData().isEmpty(), "Absent client whale data must decode real bundled species");
            helper.assertTrue(ComplexMob.ENTITY_DATA_HASH.get(whale) == decoded, "Fallback must populate the production cache");
            helper.assertTrue(UntamedClientEntityData.load(whale, Dist.CLIENT) == decoded, "Fallback must preserve cached identity");
            helper.assertTrue(ComplexMob.getEntityData(whale) == decoded, "ComplexMob consumer must see fallback data");
            helper.succeed();
        } finally {
            ComplexMob.ENTITY_DATA_HASH.remove(whale);
            if (previous != null) ComplexMob.ENTITY_DATA_HASH.put(whale, previous);
        }
    }
}
