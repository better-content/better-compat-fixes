package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Pack policy deliberately limited to Minecraft's two vanilla boat entity types. */
public final class VanillaBoatPolicy {
    private VanillaBoatPolicy() {
    }

    public static boolean appliesTo(final Boat boat) {
        return appliesTo(boat.getType());
    }

    public static boolean appliesTo(final EntityType<?> type) {
        return type == EntityType.BOAT || type == EntityType.CHEST_BOAT;
    }

    public static float destructionThreshold(final Boat boat, final float vanillaThreshold) {
        if (!appliesTo(boat)) {
            return vanillaThreshold;
        }
        return (float) (vanillaThreshold * BcFixesConfig.vanillaBoatDurabilityMultiplier());
    }

    public static boolean suppressVesselDrop(final Boat boat) {
        return appliesTo(boat) && BcFixesConfig.vanillaBoatSuppressDestructionDrop();
    }

    /** Returns a partial material refund; it deliberately cannot recreate the destroyed vessel. */
    public static List<ItemStack> destructionComponents(final Boat boat) {
        if (!suppressVesselDrop(boat)) {
            return List.of();
        }
        final ItemStack planks = new ItemStack(boat.getVariant().getPlanks(), 3);
        if (boat instanceof ChestBoat) {
            return List.of(planks, new ItemStack(Items.CHEST));
        }
        return List.of(planks);
    }
}
