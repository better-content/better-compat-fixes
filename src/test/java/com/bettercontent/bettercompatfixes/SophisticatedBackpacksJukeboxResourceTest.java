package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class SophisticatedBackpacksJukeboxResourceTest {
    @Test
    void legacyJukeboxGuardIsBackpackSpecificAndGated() throws Exception {
        final var config = JsonParser.parseReader(Files.newBufferedReader(
                Path.of("src/main/resources/better_compat_fixes.mixins.json"))).getAsJsonObject();
        final String mixins = config.getAsJsonArray("mixins").toString();
        final String plugin = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/BetterContentMixinPlugin.java"));
        final String owner = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/sophisticatedbackpacks/UpgradeWrapperBaseMixin.java"));
        final String wrapper = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/sophisticatedbackpacks/JukeboxUpgradeWrapperMixin.java"));
        final String menu = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/sophisticatedbackpacks/JukeboxUpgradeContainerMixin.java"));

        assertTrue(mixins.contains("sophisticatedbackpacks.JukeboxUpgradeContainerMixin"));
        assertTrue(mixins.contains("sophisticatedbackpacks.JukeboxUpgradeWrapperMixin"));
        assertTrue(mixins.contains("sophisticatedbackpacks.UpgradeWrapperBaseMixin"));
        assertTrue(plugin.contains("getModFileById(\"sophisticatedbackpacks\")"));
        assertTrue(owner.contains("instanceof BackpackWrapper"));
        assertTrue(owner.contains("UpgradeWrapperBase.class"));
        assertTrue(wrapper.contains("method = \"isEnabled\""));
        assertTrue(wrapper.contains("method = \"tick\""));
        assertTrue(wrapper.contains("method = \"play(Lnet/minecraft/world/entity/Entity;)V\""));
        assertTrue(menu.contains("SophisticatedBackpackJukeboxAccess"));
    }
}
