package com.bettercontent.bettercompatfixes.compat;

import com.wdiscute.starcatcher.io.ModDataComponents;
import com.wdiscute.starcatcher.registry.custom.catchmodifiers.ModCatchModifiers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;

/** Uses the same native default-component mechanism as Starcatcher's own basic worm. */
public final class StarcatcherSpoilage {
    private StarcatcherSpoilage() {}

    public static void register() {
        var meat = new ResourceLocation("better_industrial_heat", "spoiled_meat");
        if (ForgeRegistries.ITEMS.containsKey(meat)) registerBasicBait(ForgeRegistries.ITEMS.getValue(meat));
    }

    public static void registerBasicBait(Item item) {
        var modifiers = List.of(ModCatchModifiers.DECREASES_LURE_TIME.getFirst());
        var stack = new net.minecraft.world.item.ItemStack(item);
        if (ModDataComponents.has(stack, ModDataComponents.CATCH_MODIFIERS)) {
            if (!modifiers.equals(ModDataComponents.get(stack, ModDataComponents.CATCH_MODIFIERS))) {
                throw new IllegalStateException("Conflicting spoiled-meat bait defaults for " + item);
            }
            return;
        }
        ModDataComponents.registerDefault(item, ModDataComponents.CATCH_MODIFIERS, modifiers);
    }
}
