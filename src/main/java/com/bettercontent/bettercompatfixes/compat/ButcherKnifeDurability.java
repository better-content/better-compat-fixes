package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public final class ButcherKnifeDurability {
    private static final ResourceLocation BUTCHER_KNIFE =
            ResourceLocation.fromNamespaceAndPath("additionalweaponry", "butcher_knife");

    private ButcherKnifeDurability() {
    }

    /** Called only at TCon's native damageAnimated invocation after a successful removal.
     *  Adjust its one-point shears charge; native modifier/unbreaking and break signaling remain owned by TCon. */
    public static int nativeCutDamage(int nativeDamage, ItemStack tool, BlockState cut, ServerPlayer player) {
        if (nativeDamage != 1 || player == null || player.isCreative() || player.getMainHandItem() != tool
                || !BUTCHER_KNIFE.equals(ForgeRegistries.ITEMS.getKey(tool.getItem()))) return nativeDamage;
        if (cut.is(Blocks.GRASS) || cut.is(Blocks.TALL_GRASS)
                || cut.is(Blocks.FERN) || cut.is(Blocks.LARGE_FERN)) return 3;
        return nativeDamage;
    }
}
