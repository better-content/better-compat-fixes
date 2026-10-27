package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/** Structural/ownership checks only; native behavior is asserted by SpawnerMiningGameTests. */
class SpawnerMiningPolicyContractTest {
    @Test
    void exactBlockNativeStackPredicateAndThreeDenialOnlyListeners() throws Exception {
        final var source = Files.readString(Path.of("src/main/java/com/bettercontent/bettercompatfixes/compat/SpawnerMiningPolicy.java"));
        assertTrue(source.contains("@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID)"));
        assertTrue(source.contains("!state.is(Blocks.SPAWNER) || player.isCreative()"));
        assertTrue(source.contains("player.getMainHandItem()"));
        assertTrue(source.contains("stack.isEmpty() || !stack.isCorrectToolForDrops(state)"));
        for (final var event : new String[]{"PlayerInteractEvent.LeftClickBlock", "PlayerEvent.BreakSpeed", "BlockEvent.BreakEvent"}) {
            assertTrue(source.contains(event), event);
        }
        assertEquals(3, source.split("@SubscribeEvent", -1).length - 1);
        assertEquals(3, source.split(java.util.regex.Pattern.quote("event.setCanceled(true)"), -1).length - 1);
        assertTrue(source.contains("event.setNewSpeed(0.0F)"));
        assertTrue(source.contains("event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.ABORT"),
                "Tool switching must not prevent native abort cleanup");
        for (final var forbidden : new String[]{"setCanceled(false)", "Result.ALLOW", "Items.", "Tiers.", "getOffhandItem",
                "SpawnerBlock.class", "Explosion", "setBlock", "setDamage", "hurtAndBreak", "sendSystemMessage"}) {
            assertFalse(source.contains(forbidden), forbidden);
        }
        final var bootstrap = Files.readString(Path.of("src/main/java/com/bettercontent/bettercompatfixes/BetterContentFixes.java"));
        assertFalse(bootstrap.contains("EVENT_BUS.register(SpawnerMiningPolicy"), "Subscriber must not register twice");
    }

    @Test
    void nativeFixtureUsesOwnedControlledTierButProductionJarDoesNotShipFixtureOrTierPolicy() throws Exception {
        final var authored = Path.of("src/nativeFixture/resources/data/minecraft/tags/blocks/needs_diamond_tool.json");
        final var tag = JsonParser.parseString(Files.readString(authored)).getAsJsonObject();
        assertFalse(tag.get("replace").getAsBoolean());
        assertEquals(1, tag.getAsJsonArray("values").size());
        assertEquals("minecraft:spawner", tag.getAsJsonArray("values").get(0).getAsString());
        assertArrayEquals(Files.readAllBytes(authored), Files.readAllBytes(Path.of("build/resources/nativeFixture/data/minecraft/tags/blocks/needs_diamond_tool.json")));
        final var properties = new java.util.Properties();
        try (final var input = Files.newInputStream(Path.of("gradle.properties"))) { properties.load(input); }
        try (final var zip = new ZipFile("build/libs/better-compat-fixes-" + properties.getProperty("mod_version") + ".jar")) {
            assertNotNull(zip.getEntry("com/bettercontent/bettercompatfixes/compat/SpawnerMiningPolicy.class"));
            assertNull(zip.getEntry("data/minecraft/tags/blocks/needs_diamond_tool.json"));
            assertNull(zip.getEntry("com/bettercontent/bettercompatfixes/gametest/SpawnerMiningGameTests.class"));
        }
    }
}
