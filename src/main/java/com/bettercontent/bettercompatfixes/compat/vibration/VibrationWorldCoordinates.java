package com.bettercontent.bettercompatfixes.compat.vibration;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

/** Same conversion/empty-source behavior as the pinned original VS wrappers; no cached ship transform. */
public final class VibrationWorldCoordinates {
    private VibrationWorldCoordinates() {}
    public static Vec3 vectorToWorld(Vec3 original, ServerLevel level) {
        return VSGameUtilsKt.toWorldCoordinates(level, original);
    }
    public static PositionSource sourceToWorld(PositionSource original, ServerLevel level) {
        var position = original.getPosition(level);
        if (position.isPresent()) {
            return new BlockPositionSource(BlockPos.containing(VSGameUtilsKt.toWorldCoordinates(level, position.get())));
        }
        return original;
    }
}
