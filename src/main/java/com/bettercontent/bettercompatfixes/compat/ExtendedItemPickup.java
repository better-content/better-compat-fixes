package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Adds a horizontal-only shell to vanilla ordinary-item pickup reach. */
public final class ExtendedItemPickup {
    private ExtendedItemPickup() {
    }

    @SubscribeEvent
    public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        collectExtraItems(player, BcFixesConfig.itemsExtraPickupHorizontalRadius());
    }

    public static int collectExtraItems(final ServerPlayer player, final double extraHorizontalRadius) {
        if (extraHorizontalRadius <= 0.0D) {
            return 0;
        }
        final AABB vanilla = vanillaPickupBounds(player);
        final AABB extended = extendHorizontally(vanilla, extraHorizontalRadius);
        int touched = 0;
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, extended)) {
            if (!item.getBoundingBox().intersects(vanilla)) {
                item.playerTouch(player);
                touched++;
            }
        }
        return touched;
    }

    public static AABB vanillaPickupBounds(final ServerPlayer player) {
        final Entity vehicle = player.getVehicle();
        return vanillaPickupBounds(player.getBoundingBox(), vehicle != null && !vehicle.isRemoved()
                ? vehicle.getBoundingBox()
                : null);
    }

    public static AABB vanillaPickupBounds(final AABB playerBounds, final AABB vehicleBounds) {
        return vehicleBounds != null
                ? playerBounds.minmax(vehicleBounds).inflate(1.0D, 0.0D, 1.0D)
                : playerBounds.inflate(1.0D, 0.5D, 1.0D);
    }

    public static AABB extendHorizontally(final AABB vanilla, final double extraHorizontalRadius) {
        return vanilla.inflate(extraHorizontalRadius, 0.0D, extraHorizontalRadius);
    }
}
