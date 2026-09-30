package com.bettercontent.bettercompatfixes.api.event;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

/** A Sleeping Overhaul timelapse boundary with a provider-generated, persisted episode ID. */
public final class SleepTimelapseEvent extends Event {
    public enum Kind { STARTED, FINISHED }

    private final ServerPlayer player;
    private final Kind kind;
    private final String episodeId;
    private final ResourceKey<Level> respawnDimension;
    private final BlockPos respawnPosition;
    private final float respawnAngle;
    private final boolean respawnForced;

    public SleepTimelapseEvent(ServerPlayer player, Kind kind, String episodeId,
                              ResourceKey<Level> respawnDimension, @Nullable BlockPos respawnPosition,
                              float respawnAngle, boolean respawnForced) {
        this.player = Objects.requireNonNull(player, "player");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.episodeId = Objects.requireNonNull(episodeId, "episodeId");
        this.respawnDimension = Objects.requireNonNull(respawnDimension, "respawnDimension");
        this.respawnPosition = respawnPosition;
        this.respawnAngle = respawnAngle;
        this.respawnForced = respawnForced;
    }

    public ServerPlayer player() { return player; }
    public Kind kind() { return kind; }
    public String episodeId() { return episodeId; }
    public ResourceKey<Level> respawnDimension() { return respawnDimension; }
    public @Nullable BlockPos respawnPosition() { return respawnPosition; }
    public float respawnAngle() { return respawnAngle; }
    public boolean respawnForced() { return respawnForced; }
}
