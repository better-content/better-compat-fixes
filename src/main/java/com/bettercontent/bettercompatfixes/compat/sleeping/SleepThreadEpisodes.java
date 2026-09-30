package com.bettercontent.bettercompatfixes.compat.sleeping;

import com.bettercontent.bettercompatfixes.api.event.SleepTimelapseEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

/** Bridges Sleeping Overhaul's authoritative timelapse boundaries to optional Threads evidence. */
public final class SleepThreadEpisodes {
    private static final String ROOT = "BetterContentSleepThreadEpisode";
    private static final String TOKEN = "token";
    private static final String STARTED_PUBLISHED = "startedPublished";
    private static final String DIMENSION = "dimension";
    private static final String HAS_POSITION = "hasPosition";
    private static final String POSITION = "position";
    private static final String ANGLE = "angle";
    private static final String FORCED = "forced";
    private static MinecraftServer observedServer;
    private static boolean previouslyActive;

    private SleepThreadEpisodes() {
    }

    public static void update(final MinecraftServer server, final boolean active) {
        if (server != observedServer) {
            observedServer = server;
            previouslyActive = false;
        }

        final Transition transition = transition(previouslyActive, active);
        previouslyActive = active;
        if (transition == Transition.START) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.isSleeping()) {
                    start(player);
                }
            }
        } else if (transition == Transition.FINISH && server.overworld().isDay()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                finish(player);
            }
        }
    }

    static Transition transition(final boolean wasActive, final boolean active) {
        if (!wasActive && active) {
            return Transition.START;
        }
        if (wasActive && !active) {
            return Transition.FINISH;
        }
        return Transition.NONE;
    }

    static String episodeToken(
            final boolean continuingPersistedEpisode,
            final String persistedToken,
            final String generatedToken
    ) {
        if (continuingPersistedEpisode && validToken(persistedToken)) {
            return persistedToken;
        }
        return generatedToken;
    }

    static void start(final ServerPlayer player) {
        final CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        final CompoundTag prior = persisted.getCompound(ROOT);
        final boolean continuing = prior.getBoolean(STARTED_PUBLISHED)
                && sameRespawnContract(player, prior);
        final String token = episodeToken(
                continuing,
                prior.getString(TOKEN),
                player.getUUID() + ":sleep:" + player.server.getTickCount());
        if (!validToken(token)) {
            return;
        }

        final CompoundTag episode = respawnContract(player);
        episode.putString(TOKEN, token);
        final boolean alreadyPublished = continuing;
        episode.putBoolean(STARTED_PUBLISHED, true);
        persisted.put(ROOT, episode);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        if (!alreadyPublished) {
            MinecraftForge.EVENT_BUS.post(event(player, SleepTimelapseEvent.Kind.STARTED, token));
        }
    }

    static void finish(final ServerPlayer player) {
        final CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(ROOT)) {
            return;
        }
        final CompoundTag episode = persisted.getCompound(ROOT);
        if (!sameRespawnContract(player, episode)) {
            return;
        }
        final String token = episode.getString(TOKEN);
        if (!validToken(token) || !episode.getBoolean(STARTED_PUBLISHED)) {
            return;
        }
        persisted.remove(ROOT);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        MinecraftForge.EVENT_BUS.post(event(player, SleepTimelapseEvent.Kind.FINISHED, token));
    }

    private static SleepTimelapseEvent event(
            final ServerPlayer player,
            final SleepTimelapseEvent.Kind kind,
            final String token
    ) {
        return new SleepTimelapseEvent(
                player,
                kind,
                token,
                player.getRespawnDimension(),
                player.getRespawnPosition(),
                player.getRespawnAngle(),
                player.isRespawnForced());
    }

    private static CompoundTag respawnContract(final ServerPlayer player) {
        final CompoundTag contract = new CompoundTag();
        contract.putString(DIMENSION, player.getRespawnDimension().location().toString());
        final BlockPos position = player.getRespawnPosition();
        contract.putBoolean(HAS_POSITION, position != null);
        if (position != null) {
            contract.putLong(POSITION, position.asLong());
        }
        contract.putInt(ANGLE, Float.floatToIntBits(player.getRespawnAngle()));
        contract.putBoolean(FORCED, player.isRespawnForced());
        return contract;
    }

    private static boolean sameRespawnContract(final ServerPlayer player, final CompoundTag expected) {
        final BlockPos position = player.getRespawnPosition();
        return expected.getString(DIMENSION).equals(player.getRespawnDimension().location().toString())
                && expected.getBoolean(HAS_POSITION) == (position != null)
                && (position == null || expected.getLong(POSITION) == position.asLong())
                && expected.getInt(ANGLE) == Float.floatToIntBits(player.getRespawnAngle())
                && expected.getBoolean(FORCED) == player.isRespawnForced();
    }

    private static boolean validToken(final String value) {
        if (value == null || value.isBlank() || value.length() > 128) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);
            if (character < 0x21 || character > 0x7e) {
                return false;
            }
        }
        return true;
    }

    enum Transition {
        START,
        FINISH,
        NONE
    }

}
