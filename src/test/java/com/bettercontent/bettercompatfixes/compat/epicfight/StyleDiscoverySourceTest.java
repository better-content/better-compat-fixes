package com.bettercontent.bettercompatfixes.compat.epicfight;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class StyleDiscoverySourceTest {
    @Test
    void basicAttackDiscoversNativeComboAndPublishesSharedNotice() throws IOException {
        final Path root = Path.of("src/main/java/com/bettercontent/bettercompatfixes");
        final String mixin = Files.readString(root.resolve("mixin/epicfight/ServerPlayerPatchMixin.java"));
        final String discovery = Files.readString(root.resolve("compat/epicfight/StyleDiscovery.java"));
        final String mixins = Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json"));

        assertTrue(mixin.contains("EventType.BASIC_ATTACK_EVENT"));
        assertTrue(mixin.contains("StyleDiscovery.observeAttackStart("));
        assertTrue(discovery.contains("patch.getHoldingItemCapability"));
        assertTrue(discovery.contains("patch.getAttackingHand()"));
        assertTrue(discovery.contains("player.getItemInHand(hand).copy()"));
        assertTrue(discovery.contains("StyleCatalogue.observe(patch, held, capability)"));
        assertTrue(discovery.contains("GameplayNotices.send"));
        assertTrue(!discovery.contains("displayClientMessage"));
        assertTrue(mixins.contains("\"epicfight.ServerPlayerPatchMixin\""));
    }
}
