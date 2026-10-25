package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Observe the real upstream startup registration; never call or duplicate the repair/listener. */
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class VillageAnimalPoolGameTests {
    @SubscribeEvent public static void register(RegisterGameTestsEvent event) { event.register(VillageAnimalPoolGameTests.class); }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "village_animals", timeoutTicks = 20)
    public static void originalListenerRegistersAllFiveRawAndThirteenWeightedEntries(GameTestHelper helper) {
        boolean absent = Boolean.getBoolean("better_compat_fixes.expectGeneticAnimalsAbsent");
        helper.assertTrue(ModList.get().isLoaded("eanimod") != absent, "Genetic Animals fixture population differs");
        var pool = helper.getLevel().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL)
                .get(new ResourceLocation("minecraft", "village/common/animals"));
        helper.assertTrue(pool != null, "Native village animal pool missing");
        helper.assertTrue((pool.rawTemplates instanceof ImmutableList<?>) == absent,
                "Only the present pinned listener may require a mutable raw-list copy");
        var expected = Map.of("chickens_1", 7, "cows_2", 1, "cows_3", 1, "llamas_1", 3, "sheep_3", 1);
        var ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        var weighted = pool.getShuffledTemplates(RandomSource.create(0));
        helper.assertTrue(weighted.size() == pool.rawTemplates.stream().mapToInt(pair -> pair.getSecond()).sum(),
                "Native raw weights and expanded template population differ");
        for (var entry : expected.entrySet()) {
            String location = "minecraft:village/common/animals/" + entry.getKey();
            var raw = pool.rawTemplates.stream().filter(pair -> location.equals(location(pair.getFirst(), ops))).toList();
            long expanded = weighted.stream().filter(element -> location.equals(location(element, ops))).count();
            helper.assertTrue(raw.size() == (absent ? 0 : 1), "Duplicate or missing native raw village animal entry: " + location);
            if (!absent) helper.assertTrue(raw.get(0).getSecond().equals(entry.getValue()), "Native village animal raw weight changed: " + location);
            helper.assertTrue(expanded == (absent ? 0 : entry.getValue()), "Native village animal expanded weight changed: " + location);
        }
        helper.succeed();
    }

    private static String location(StructurePoolElement element, RegistryOps<com.google.gson.JsonElement> ops) {
        var value = StructurePoolElement.CODEC.encodeStart(ops, element)
                .getOrThrow(false, message -> { throw new AssertionError(message); }).getAsJsonObject();
        return value.has("location") && value.get("location").isJsonPrimitive() ? value.get("location").getAsString() : "";
    }
}
