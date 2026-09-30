package com.bettercontent.bettercompatfixes.api.event;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** A ParCool control-learning episode boundary owned by Better Content Fixes. */
public final class CustomControlEpisodeEvent extends Event {
    public enum Kind { FIRST_ACCEPTED, DISTINCT_SECOND }

    private final ServerPlayer player;
    private final Kind kind;
    private final String acceptedActionId;
    private final String firstActionId;
    private final String episodeId;

    public CustomControlEpisodeEvent(ServerPlayer player, Kind kind, String acceptedActionId,
                                     String firstActionId, String episodeId) {
        this.player = Objects.requireNonNull(player, "player");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.acceptedActionId = Objects.requireNonNull(acceptedActionId, "acceptedActionId");
        this.firstActionId = Objects.requireNonNull(firstActionId, "firstActionId");
        this.episodeId = Objects.requireNonNull(episodeId, "episodeId");
    }

    public ServerPlayer player() { return player; }
    public Kind kind() { return kind; }
    public String acceptedActionId() { return acceptedActionId; }
    public String firstActionId() { return firstActionId; }
    public String episodeId() { return episodeId; }
}
