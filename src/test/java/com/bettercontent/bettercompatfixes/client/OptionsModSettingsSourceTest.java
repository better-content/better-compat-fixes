package com.bettercontent.bettercompatfixes.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionsModSettingsSourceTest {
    private static final Path ROOT = Path.of(".");

    @Test
    void optionsScreenGetsConfiguredModSettingsEntryWithoutRewritingControls() throws Exception {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/com/bettercontent/bettercompatfixes/client/settings/OptionsModSettingsEntry.java"));
        String translations = Files.readString(ROOT.resolve(
                "src/main/resources/assets/better_compat_fixes/lang/en_us.json"));
        String configuredPin = Files.readString(ROOT.resolve("../../better-content-modpack/mods/configured.pw.toml"));

        assertTrue(source.contains("instanceof OptionsScreen"));
        assertTrue(source.contains("ModList.get().isLoaded(\"configured\")"));
        assertTrue(source.contains("new ModListScreen(options)"));
        assertTrue(source.contains("options.width - BUTTON_WIDTH - EDGE_PADDING"));
        assertFalse(source.contains("KeyMapping"), "customized gameplay key bindings must remain untouched");
        assertTrue(translations.contains("options.better_compat_fixes.mod_settings"));
        assertTrue(configuredPin.contains("filename = \"configured-forge-1.20.1-2.2.3.jar\""));
    }
}
