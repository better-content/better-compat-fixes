package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.*;
import com.bettercontent.bettercompatfixes.client.tooltip.*;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

public class ToolTooltipTest {
    private static final Path JAVA = Path.of("src/main/java/com/bettercontent/bettercompatfixes");
    private static final Path RESOURCE = Path.of("src/main/resources/assets/better_compat_fixes/tooltips/effects.json");

    @Test void affixAttackSpeedUsesItemAttributeExactlyOnceAndVanillaOperationOrder() {
        UUID speed = UUID.randomUUID(), affix = UUID.randomUUID(), multiply = UUID.randomUUID();
        var values = List.of(new IntrinsicAttributeMath.Contribution(speed, -2.4, 0),
                new IntrinsicAttributeMath.Contribution(affix, 0.32, 0),
                new IntrinsicAttributeMath.Contribution(affix, 0.32, 0));
        assertEquals(1.92, IntrinsicAttributeMath.apply(4, values), 1e-8);
        var withBaseMultiplier = new ArrayList<>(values);
        withBaseMultiplier.add(new IntrinsicAttributeMath.Contribution(multiply, 0.25, 1));
        assertEquals(2.4, IntrinsicAttributeMath.apply(4, withBaseMultiplier), 1e-8);
        withBaseMultiplier.add(new IntrinsicAttributeMath.Contribution(UUID.randomUUID(), -0.5, 2));
        assertEquals(1.2, IntrinsicAttributeMath.apply(4, withBaseMultiplier), 1e-8);
        assertEquals(4, IntrinsicAttributeMath.apply(4, List.of()));
    }

    @Test void semanticFilteringNeverCutsDistinctEffectsToALineBudget() {
        var effects = new ArrayList<ToolTooltipEffect>();
        var combat = effect("mixed:combat@1", Set.of("combat"), "Conditional combat bonus");
        effects.add(combat); effects.add(combat);
        effects.add(effect("mixed:mining@1", Set.of("mining"), "Conditional mining bonus"));
        for (int i = 0; i < 12; i++) effects.add(effect("distinct:" + i, Set.of(), "Effect " + i));
        var snapshot = new ToolTooltipSnapshot(Component.literal("Name"), Set.of("combat"), List.of(),
                List.of(Component.literal("Damage 5")), List.of(), effects, List.of(Component.literal("Authored lore")));
        var rows = ToolTooltipRenderer.render(snapshot).stream().map(Component::getString).toList();
        assertEquals(16, rows.size());
        assertEquals(1, rows.stream().filter("Conditional combat bonus"::equals).count());
        assertTrue(rows.stream().noneMatch(s -> s.contains("mining")));
        assertEquals("Authored lore", rows.get(rows.size() - 1));
        assertFalse(String.join("\n", rows).contains("more effects"));
    }

    @Test void restrictionsPrecedeBehaviorAndRepresentedStatsDoNotRepeat() {
        var drawback = new ToolTooltipEffect("drawback", Set.of(), ToolTooltipEffect.Category.RESTRICTION,
                Component.literal("Destroys itself on landing"), false);
        var stat = new ToolTooltipEffect("damage", Set.of(), ToolTooltipEffect.Category.BEHAVIOR,
                Component.literal("Extra damage"), true);
        var snapshot = new ToolTooltipSnapshot(Component.literal("Ammo"), Set.of("ammo"), List.of(), List.of(), List.of(),
                List.of(effect("a", Set.of(), "Returns"), drawback, stat), List.of());
        assertEquals(List.of("Ammo", "Destroys itself on landing", "Returns"), ToolTooltipRenderer.render(snapshot).stream().map(Component::getString).toList());
    }

    @Test void catalogueHas44ActionProfilesAndMixedCapabilitiesRatherThanWholeNameFiltering() throws Exception {
        var json = JsonParser.parseString(Files.readString(RESOURCE)).getAsJsonObject();
        var data = ToolTooltipCatalogue.parse(json);
        assertEquals(44, data.actions().keySet().stream().filter(k -> !k.startsWith("skill:")).count());
        for (String id : List.of("tconstruct:jagged", "tconstruct:stonebound", "tconstruct:dwarven", "tconstruct:momentum", "etstlib:momentum_accelerate")) {
            var fragments = data.modifiers().get(id);
            assertTrue(fragments.stream().anyMatch(f -> f.requires().contains("mining")), id);
            assertTrue(fragments.stream().anyMatch(f -> !f.requires().contains("mining")), id);
        }
        assertTrue(data.modifiers().get("better_tinkers_loot_affixes:affix_stats").isEmpty());
        assertTrue(data.modifiers().get("tconstruct:sharpness").isEmpty());
        assertFalse(data.modifiers().get("tconstruct:necrotic").isEmpty());
        assertFalse(data.modifiers().get("epicfighttinkercompat:arrow_tempest").isEmpty(), "Empty data modules may have external behavior");
        assertFalse(data.modifiers().get("tconstruct:snow_boots").isEmpty(), "A hidden native entry can still have meaningful behavior");
        assertThrows(UnsupportedOperationException.class, () -> data.modifiers().clear());
    }

    @Test void hoverBoundaryIsDetachedOptionalAndBelowEmiAndLeavesBenchApisAlone() throws Exception {
        String boundary = Files.readString(JAVA.resolve("mixin/tooltip/ItemStackTooltipMixin.java"));
        assertTrue(boundary.contains("priority = 100"));
        assertTrue(boundary.contains("@WrapMethod(method = \"getTooltipLines\")"));
        assertTrue(boundary.contains("original.call(player, flag)"));
        String owner = Files.readString(JAVA.resolve("client/tooltip/ToolTooltipOwner.java"));
        assertTrue(owner.contains("original.copy()"));
        String provider = Files.readString(JAVA.resolve("client/tooltip/ToolTooltipSummaryProvider.java"));
        assertTrue(provider.contains("ToolStack.copyFrom(detached)"));
        assertTrue(provider.contains("ToolCapabilities.hasMiningDefinition(item.getToolDefinition())"));
        assertTrue(provider.contains("ToolStack.isInitialized(detached)"));
        try (var files = Files.walk(JAVA.resolve("client/tooltip"))) {
            for (var path : files.filter(Files::isRegularFile).toList()) {
                String text = Files.readString(path);
                for (String forbidden : List.of("getStatInformation(", "TooltipUtil", "FakePlayer", "getInnateSkill(", "Screen.hasShiftDown(", "Screen.hasAltDown(", "getStyle(null)", "Minecraft.getInstance().player", "ensureHasData(", "getDescriptionList("))
                    assertFalse(text.contains(forbidden), path + " contains " + forbidden);
            }
        }
        String helper = Files.readString(JAVA.resolve("compat/tconstruct/ToolCapabilities.java"));
        assertTrue(helper.contains("hasHook(ToolHooks.IS_EFFECTIVE)"));
        assertFalse(helper.contains("isBroken"));
        assertFalse(helper.contains("getStats"));
        assertFalse(helper.contains("TinkerTags"));
    }

    private static ToolTooltipEffect effect(String key, Set<String> capabilities, String text) {
        return new ToolTooltipEffect(key, capabilities, ToolTooltipEffect.Category.BEHAVIOR, Component.literal(text), false);
    }
}
