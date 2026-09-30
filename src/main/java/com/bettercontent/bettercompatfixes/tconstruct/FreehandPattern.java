package com.bettercontent.bettercompatfixes.tconstruct;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.common.TinkerTags;

/** Provides the synthetic blank pattern used by the patternless Part Builder mixins. */
public final class FreehandPattern {
    private static final ResourceLocation PATTERN_ID = new ResourceLocation("tconstruct", "pattern");
    private static final TagKey<Item> COLORLESS_SAND = ItemTags.create(new ResourceLocation("forge", "sand/colorless"));
    private static final TagKey<Item> RED_SAND = ItemTags.create(new ResourceLocation("forge", "sand/red"));

    private FreehandPattern() {}

    public static ItemStack syntheticIfEmpty(ItemStack stack) {
        if (!stack.isEmpty()) return stack;
        Item pattern = ForgeRegistries.ITEMS.getValue(PATTERN_ID);
        return pattern == null || pattern == Items.AIR ? ItemStack.EMPTY : new ItemStack(pattern);
    }

    /** Returns true when the visible input should stand in for the hidden pattern slot. */
    public static boolean usesVisibleCastPattern(ItemStack storedPattern, ItemStack visibleInput) {
        return storedPattern.isEmpty() && isSandCastingPattern(visibleInput);
    }

    /** Resolves either legacy physical input, visible sand-cast input, or the synthetic freehand pattern. */
    public static ItemStack resolvePattern(ItemStack storedPattern, ItemStack visibleInput) {
        if (usesVisibleCastPattern(storedPattern, visibleInput)) return visibleInput;
        return syntheticIfEmpty(storedPattern);
    }

    /** Prevents visible sand-cast input from also being interpreted as a part material. */
    public static ItemStack resolveMaterial(ItemStack storedPattern, ItemStack visibleInput) {
        return usesVisibleCastPattern(storedPattern, visibleInput) ? ItemStack.EMPTY : visibleInput;
    }

    private static boolean isSandCastingPattern(ItemStack stack) {
        return stack.is(COLORLESS_SAND)
                || stack.is(RED_SAND)
                || stack.is(TinkerTags.Items.SAND_CASTS)
                || stack.is(TinkerTags.Items.RED_SAND_CASTS);
    }
}
