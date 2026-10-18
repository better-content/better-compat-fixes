package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SaulomonkSpawnDistanceTest {
    private static CompoundTag point(int x, int y, int z) {
        var data = new CompoundTag();
        data.putString("better_spawns:respawn_dim", "minecraft:overworld");
        data.putInt("better_spawns:respawn_x", x);
        data.putInt("better_spawns:respawn_y", y);
        data.putInt("better_spawns:respawn_z", z);
        return data;
    }

    @Test void distanceIsHorizontalAndTranslatedToSelectedSpawn() {
        assertEquals(2000.0, SaulomonkSpawnDistance.MINIMUM_DISTANCE);
        var data = point(10000, 30000, -20000);
        for (int distance : new int[] {1999, 2000, 2001}) {
            double actual = SaulomonkSpawnDistance.fromSelectedPoint(data, "minecraft:overworld", 10000 + distance, -20000);
            assertEquals(distance, actual);
            assertEquals(distance > 2000, actual > SaulomonkSpawnDistance.MINIMUM_DISTANCE);
        }
        assertEquals(2000.0, SaulomonkSpawnDistance.fromSelectedPoint(data, "minecraft:overworld", 11200, -18400));
        assertEquals(0.0, SaulomonkSpawnDistance.fromSelectedPoint(data, "minecraft:overworld", 10000, -20000));
    }

    @Test void missingMalformedAndDifferentDimensionPointsFailClosed() {
        assertEquals(0.0, SaulomonkSpawnDistance.fromSelectedPoint(new CompoundTag(), "minecraft:overworld", 100000, 100000));
        for (String key : new String[] {"respawn_dim", "respawn_x", "respawn_y", "respawn_z"}) {
            var data = point(0, 64, 0);
            data.remove("better_spawns:" + key);
            assertEquals(0.0, SaulomonkSpawnDistance.fromSelectedPoint(data, "minecraft:overworld", 100000, 100000));
        }
        var data = point(0, 64, 0);
        assertEquals(0.0, SaulomonkSpawnDistance.fromSelectedPoint(data, "minecraft:the_nether", 100000, 100000));
        data.putString("better_spawns:respawn_x", "not an integer");
        assertEquals(0.0, SaulomonkSpawnDistance.fromSelectedPoint(data, "minecraft:overworld", 100000, 100000));
    }

    @Test void extremeCoordinatesDoNotOverflowIntegerSubtraction() {
        assertEquals(4294967295.0, SaulomonkSpawnDistance.fromSelectedPoint(
                point(Integer.MIN_VALUE, 64, 0), "minecraft:overworld", Integer.MAX_VALUE, 0));
    }
}
