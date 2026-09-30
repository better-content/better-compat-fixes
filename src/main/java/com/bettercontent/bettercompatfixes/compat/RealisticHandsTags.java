package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class RealisticHandsTags {
    public static final TagKey<Block> AXE = blockTag("axe");
    public static final TagKey<Item> AXE_TOOLS = itemTag("axe");

    private RealisticHandsTags() {
    }

    private static TagKey<Block> blockTag(final String name) {
        return TagKey.create(Registries.BLOCK, id("realistic_hands/" + name));
    }

    private static TagKey<Item> itemTag(final String name) {
        return TagKey.create(Registries.ITEM, id("realistic_hands/tools/" + name));
    }

    private static ResourceLocation id(final String path) {
        return new ResourceLocation(BetterContentFixes.MOD_ID, path);
    }
}
