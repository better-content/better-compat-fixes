package com.bettercontent.bettercompatfixes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyModifier;
import org.junit.jupiter.api.Test;

final class RecommendedKeymapMigratorTest {
    @Test
    void exactLegacyBindingsMigrateToTheRecommendedProfile() {
        final Map<String, KeyMapping> mappings = legacyMappings();

        assertTrue(RecommendedKeymapMigrator.applyLegacyMappings(mappings));

        for (final RecommendedKeymapMigrator.BindingMigration migration
                : RecommendedKeymapMigrator.MIGRATIONS) {
            final KeyMapping mapping = mappings.get(migration.name());
            assertEquals(migration.desiredKey(), mapping.getKey(), migration.name());
            assertEquals(migration.desiredModifier(), mapping.getKeyModifier(), migration.name());
        }
    }

    @Test
    void customizedBindingsArePreservedWhileOtherLegacyBindingsMigrate() {
        final Map<String, KeyMapping> mappings = legacyMappings();
        final RecommendedKeymapMigrator.BindingMigration customized = RecommendedKeymapMigrator.MIGRATIONS.get(0);
        final InputConstants.Key customKey = InputConstants.Type.KEYSYM.getOrCreate(71);
        mappings.get(customized.name()).setKeyModifierAndCode(KeyModifier.ALT, customKey);

        assertTrue(RecommendedKeymapMigrator.applyLegacyMappings(mappings));
        assertEquals(customKey, mappings.get(customized.name()).getKey());
        assertEquals(KeyModifier.ALT, mappings.get(customized.name()).getKeyModifier());
    }

    @Test
    void anAlreadyMigratedProfileIsIdempotent() {
        final Map<String, KeyMapping> mappings = legacyMappings();
        assertTrue(RecommendedKeymapMigrator.applyLegacyMappings(mappings));
        assertFalse(RecommendedKeymapMigrator.applyLegacyMappings(mappings));
    }

    @Test
    void versionOneToTwoMigrationTouchesOnlyVersionTwoDefaults() {
        final Map<String, KeyMapping> mappings = legacyMappings();
        final RecommendedKeymapMigrator.BindingMigration oldProfile = RecommendedKeymapMigrator.MIGRATIONS.get(0);

        assertTrue(RecommendedKeymapMigrator.applyLegacyMappings(mappings, 1));
        assertEquals(oldProfile.legacyKey(), mappings.get(oldProfile.name()).getKey());
        for (final RecommendedKeymapMigrator.BindingMigration migration
                : RecommendedKeymapMigrator.MIGRATIONS) {
            if (migration.profileVersion() == 2) {
                assertEquals(migration.desiredKey(), mappings.get(migration.name()).getKey(), migration.name());
                assertEquals(migration.desiredModifier(), mappings.get(migration.name()).getKeyModifier(), migration.name());
            }
        }
    }

    @Test
    void versionTwoCustomizedBindingSurvivesUpgrade() {
        final Map<String, KeyMapping> mappings = legacyMappings();
        final RecommendedKeymapMigrator.BindingMigration migration = RecommendedKeymapMigrator.MIGRATIONS.stream()
                .filter(candidate -> candidate.profileVersion() == 2)
                .findFirst()
                .orElseThrow();
        final InputConstants.Key custom = InputConstants.Type.KEYSYM.getOrCreate(71);
        mappings.get(migration.name()).setKeyModifierAndCode(KeyModifier.CONTROL, custom);

        assertTrue(RecommendedKeymapMigrator.applyLegacyMappings(mappings, 1));
        assertEquals(custom, mappings.get(migration.name()).getKey());
        assertEquals(KeyModifier.CONTROL, mappings.get(migration.name()).getKeyModifier());
    }

    private static Map<String, KeyMapping> legacyMappings() {
        final Map<String, KeyMapping> mappings = new LinkedHashMap<>();
        for (final RecommendedKeymapMigrator.BindingMigration migration
                : RecommendedKeymapMigrator.MIGRATIONS) {
            final KeyMapping mapping = new KeyMapping(
                    migration.name(),
                    migration.legacyKey().getType(),
                    migration.legacyKey().getValue(),
                    "key.categories.better_compat_fixes_test");
            mapping.setKeyModifierAndCode(migration.legacyModifier(), migration.legacyKey());
            mappings.put(migration.name(), mapping);
        }
        return mappings;
    }
}
