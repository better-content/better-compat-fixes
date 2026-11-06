package com.bettercontent.bettercompatfixes.client.tooltip;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

/** Bounded presentation regressions: missing data must not imply fabricated statistics. */
class ToolTooltipDegradedTest {
    @Test void missingDefinitionRetainsNameLoreAndExistingStackFacts() {
        var tag = new CompoundTag();
        var materials = new ListTag();
        materials.add(StringTag.valueOf("tconstruct:iron"));
        tag.put("tic_materials", materials);
        var before = tag.copy();
        var snapshot = ToolTooltipSummaryProvider.unavailable(tag, Component.literal("Named tool"),
                List.of(Component.literal("Handwritten lore").withStyle(ChatFormatting.LIGHT_PURPLE)),
                "definition_pending", "Tool definition not available yet");
        var rows = ToolTooltipRenderer.render(snapshot);
        assertEquals("Named tool", rows.get(0).getString());
        assertTrue(rows.stream().anyMatch(c -> c.getString().contains("Tool definition not available yet")));
        assertTrue(rows.stream().anyMatch(c -> c.getString().contains("Stored material parts") && c.getString().contains("1")));
        assertEquals("Handwritten lore", rows.get(rows.size() - 1).getString());
        assertTrue(snapshot.performance().isEmpty(), "No invented performance on uninitialized data");
        assertEquals(before, tag, "No tag mutation from degraded hover");
    }

    @Test void accentsPreserveColoredModifierNamesAndLore() {
        var modifier = Component.literal("Rosegold").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(": faster mining"));
        var effect = new ToolTooltipEffect("trait", Set.of(), ToolTooltipEffect.Category.BEHAVIOR, modifier, false);
        var conditional = new ToolTooltipEffect("conditional", Set.of(), ToolTooltipEffect.Category.CONDITIONAL,
                Component.literal("When wet"), false);
        var restriction = new ToolTooltipEffect("restriction", Set.of(), ToolTooltipEffect.Category.RESTRICTION,
                Component.literal("Cannot block"), false);
        var lore = Component.literal("An old promise").withStyle(ChatFormatting.DARK_PURPLE);
        var snapshot = new ToolTooltipSnapshot(Component.literal("Tool"), Set.of(), List.of(),
                List.of(ToolTooltipRenderer.fact("speed", "Speed", "7")),
                List.of(Component.literal("Special action")), List.of(effect, conditional, restriction), List.of(lore));
        var rows = ToolTooltipRenderer.render(snapshot);
        var value = ((TranslatableContents)rows.get(1).getContents()).getArgs()[0];
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.AQUA), ((Component)value).getStyle().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.AQUA), rows.get(2).getStyle().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), rows.get(3).getStyle().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD), rows.get(4).getStyle().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE), rows.get(5).getStyle().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_PURPLE), rows.get(6).getStyle().getColor());
    }
}
