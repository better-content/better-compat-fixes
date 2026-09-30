package com.bettercontent.bettercompatfixes.client;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.config.BcFixesClientConfig;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = BetterContentFixes.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RecommendedKeymapMigrator {
    private static final Logger LOGGER = LogManager.getLogger();
    static final List<BindingMigration> MIGRATIONS = List.of(
            keyboard(1, "key.parcool.FastRun", GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_LEFT_SHIFT),
            mouse(1, "key.parcool.ClingToCliff", 1, 4),
            mouse(1, "key.parcool.Vault", 1, 4),
            mouse(1, "key.parcool.RideZipline", 1, 4),
            mouse(1, "key.parcool.HangDown", 1, 4),
            mouse(1, "key.parcool.WallSlide", 1, 4),
            mouse(1, "ping-wheel.key.ping-location", 4, 3),
            new BindingMigration(
                    1,
                    "key.epicfight.dodge",
                    mouseKey(3),
                    KeyModifier.NONE,
                    InputConstants.UNKNOWN,
                    KeyModifier.NONE),
            new BindingMigration(
                    1,
                    "key.epicfight.lock_on",
                    mouseKey(4),
                    KeyModifier.NONE,
                    keyboardKey(GLFW.GLFW_KEY_CAPS_LOCK),
                    KeyModifier.NONE),
            new BindingMigration(
                    1,
                    "key.epicfight.lock_on_shift_freely",
                    mouseKey(4),
                    KeyModifier.SHIFT,
                    keyboardKey(GLFW.GLFW_KEY_CAPS_LOCK),
                    KeyModifier.SHIFT),
            modifiedKeyboard(2, "key.rotavision.rotate", GLFW.GLFW_KEY_R, KeyModifier.NONE,
                    GLFW.GLFW_KEY_R, KeyModifier.ALT),
            modifiedKeyboard(2, "key.rotavision.toggle", GLFW.GLFW_KEY_H, KeyModifier.NONE,
                    GLFW.GLFW_KEY_T, KeyModifier.ALT),
            modifiedKeyboard(2, "key.relics.ability_list", GLFW.GLFW_KEY_R, KeyModifier.ALT,
                    GLFW.GLFW_KEY_A, KeyModifier.ALT),
            modifiedKeyboard(2, "key.moreartifacts.eye.teleport", GLFW.GLFW_KEY_T, KeyModifier.ALT,
                    GLFW.GLFW_KEY_P, KeyModifier.ALT),
            new BindingMigration(
                    2,
                    "quark.keybind.lock_rotation",
                    keyboardKey(GLFW.GLFW_KEY_K),
                    KeyModifier.ALT,
                    InputConstants.UNKNOWN,
                    KeyModifier.NONE));

    private static boolean attempted;

    private RecommendedKeymapMigrator() {
    }

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (attempted || event.phase != TickEvent.Phase.END || !BcFixesClientConfig.shouldMigrateRecommendedKeymap()) {
            return;
        }
        if (!supportedPackVersionsLoaded()) {
            attempted = true;
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options == null) {
            return;
        }
        attempted = true;

        final Map<String, KeyMapping> mappings = new LinkedHashMap<>();
        Arrays.stream(minecraft.options.keyMappings).forEach(mapping -> mappings.put(mapping.getName(), mapping));
        final int priorVersion = BcFixesClientConfig.KEYMAP_PROFILE_VERSION.get();
        final List<String> missing = MIGRATIONS.stream()
                .filter(migration -> migration.profileVersion() > priorVersion)
                .map(BindingMigration::name)
                .filter(name -> !mappings.containsKey(name))
                .toList();
        if (!missing.isEmpty()) {
            LOGGER.warn("Recommended control migration deferred; missing mappings: {}", missing);
            return;
        }

        final boolean changed = applyLegacyMappings(mappings, priorVersion);
        if (changed) {
            KeyMapping.resetMapping();
            minecraft.options.save();
        }
        BcFixesClientConfig.KEYMAP_PROFILE_VERSION.set(BcFixesClientConfig.CURRENT_KEYMAP_PROFILE_VERSION);
        BcFixesClientConfig.KEYMAP_PROFILE_VERSION.save();
        LOGGER.info("Recommended control profile migration completed{}", changed ? " with binding updates" : "");
    }

    private static boolean supportedPackVersionsLoaded() {
        return exactVersion("parcool", "3.4.3.3")
                && exactVersion("pingwheel", "1.10.1")
                && exactVersion("epicfight", "20.14.17")
                && exactVersion("rotavision", "1.0.2")
                && exactVersion("relics", "0.8.0.13")
                && exactVersion("moreartifacts", "1.5.5")
                && exactVersion("quark", "4.0-462");
    }

    static boolean applyLegacyMappings(final Map<String, KeyMapping> mappings) {
        return applyLegacyMappings(mappings, 0);
    }

    static boolean applyLegacyMappings(final Map<String, KeyMapping> mappings, final int priorVersion) {
        boolean changed = false;
        for (final BindingMigration migration : MIGRATIONS) {
            final KeyMapping mapping = migration.profileVersion() > priorVersion
                    ? mappings.get(migration.name())
                    : null;
            if (mapping != null && migration.matchesLegacy(mapping)) {
                mapping.setKeyModifierAndCode(migration.desiredModifier(), migration.desiredKey());
                changed = true;
            }
        }
        return changed;
    }

    private static boolean exactVersion(final String modId, final String version) {
        return ModList.get().getModContainerById(modId)
                .map(container -> version.equals(container.getModInfo().getVersion().toString()))
                .orElse(false);
    }

    private static BindingMigration keyboard(final int version, final String name, final int legacy, final int desired) {
        return new BindingMigration(
                version,
                name,
                keyboardKey(legacy),
                KeyModifier.NONE,
                keyboardKey(desired),
                KeyModifier.NONE);
    }

    private static BindingMigration mouse(final int version, final String name, final int legacy, final int desired) {
        return new BindingMigration(
                version,
                name,
                mouseKey(legacy),
                KeyModifier.NONE,
                mouseKey(desired),
                KeyModifier.NONE);
    }

    private static InputConstants.Key keyboardKey(final int code) {
        return InputConstants.Type.KEYSYM.getOrCreate(code);
    }

    private static InputConstants.Key mouseKey(final int code) {
        return InputConstants.Type.MOUSE.getOrCreate(code);
    }

    private static BindingMigration modifiedKeyboard(
            final int version,
            final String name,
            final int legacy,
            final KeyModifier legacyModifier,
            final int desired,
            final KeyModifier desiredModifier
    ) {
        return new BindingMigration(
                version,
                name,
                keyboardKey(legacy),
                legacyModifier,
                keyboardKey(desired),
                desiredModifier);
    }

    record BindingMigration(
            int profileVersion,
            String name,
            InputConstants.Key legacyKey,
            KeyModifier legacyModifier,
            InputConstants.Key desiredKey,
            KeyModifier desiredModifier
    ) {
        boolean matchesLegacy(final KeyMapping mapping) {
            return legacyKey.equals(mapping.getKey()) && legacyModifier == mapping.getKeyModifier();
        }
    }
}
