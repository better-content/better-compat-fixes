package com.bettercontent.bettercompatfixes.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

/** Query only: ALLOW waives a habitat/legacy restriction, never an independent spawn veto. */
@Event.HasResult
public final class GeologicalAmbientEligibilityEvent extends Event {
    private final ServerLevelAccessor level;
    private final BlockPos position;
    private final EntityType<?> type;
    private final MobSpawnType spawnType;

    public GeologicalAmbientEligibilityEvent(ServerLevelAccessor level, BlockPos position,
                                             EntityType<?> type, MobSpawnType spawnType) {
        this.level = level;
        this.position = position.immutable();
        this.type = type;
        this.spawnType = spawnType;
    }

    public ServerLevelAccessor level() { return level; }
    public BlockPos position() { return position; }
    public EntityType<?> type() { return type; }
    public MobSpawnType spawnType() { return spawnType; }

    public static boolean eligible(ServerLevelAccessor level, BlockPos position,
                                   EntityType<?> type, MobSpawnType spawnType) {
        GeologicalAmbientEligibilityEvent query = new GeologicalAmbientEligibilityEvent(level, position, type, spawnType);
        MinecraftForge.EVENT_BUS.post(query);
        return query.getResult() == Result.ALLOW;
    }
}
