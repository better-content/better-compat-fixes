package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ServerLevelAccessor;

/** Reads Better Spawns' permanent onboarding point, never beds or world origin. */
public final class SaulomonkSpawnDistance {
    public static final double MINIMUM_DISTANCE = 2000.0D;

    private SaulomonkSpawnDistance() {}

    public static double fromNearestPlayer(final ServerLevelAccessor world, final BlockPos candidate) {
        ServerPlayer nearest = null;
        double nearestSquared = Double.POSITIVE_INFINITY;
        for (ServerPlayer player : world.getLevel().players()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            double squared = player.distanceToSqr(candidate.getX() + 0.5D,
                    candidate.getY() + 0.5D, candidate.getZ() + 0.5D);
            if (squared < nearestSquared) {
                nearest = player;
                nearestSquared = squared;
            }
        }
        // Do not borrow a farther player's anchor when the nearest player is still onboarding.
        return nearest == null ? 0.0D : fromSelectedPoint(nearest.getPersistentData(),
                world.getLevel().dimension().location().toString(), candidate.getX(), candidate.getZ());
    }

    public static double fromSelectedPoint(final CompoundTag data, final String dimension,
                                          final int x, final int z) {
        if (!data.contains("better_spawns:respawn_dim", Tag.TAG_STRING)
                || !data.contains("better_spawns:respawn_x", Tag.TAG_INT)
                || !data.contains("better_spawns:respawn_y", Tag.TAG_INT)
                || !data.contains("better_spawns:respawn_z", Tag.TAG_INT)
                || !dimension.equals(data.getString("better_spawns:respawn_dim"))) return 0.0D;
        // Horizontal exploration distance: altitude must not unlock the encounter.
        double dx = (double) x - data.getInt("better_spawns:respawn_x");
        double dz = (double) z - data.getInt("better_spawns:respawn_z");
        return Math.hypot(dx, dz);
    }
}
