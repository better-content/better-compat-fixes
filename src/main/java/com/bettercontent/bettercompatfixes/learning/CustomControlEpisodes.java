package com.bettercontent.bettercompatfixes.learning;

import com.bettercontent.bettercompatfixes.api.event.CustomControlEpisodeEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

/** Owns custom-control episode identity and publishes provider-domain boundaries. */
final class CustomControlEpisodes {
    static final String PERSISTED_KEY = "BetterContentFixesCustomControlEpisode";
    static final String TOKEN_KEY = "token";
    static final String FIRST_ACTION_KEY = "firstAction";

    private CustomControlEpisodes() {
    }

    static void record(final ServerPlayer player, final String actionId) {
        final CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        final CompoundTag episode = persisted.getCompound(PERSISTED_KEY);
        final String token = episode.getString(TOKEN_KEY);
        final String firstAction = episode.getString(FIRST_ACTION_KEY);

        switch (DistinctControlEpisode.advance(token, firstAction, actionId)) {
            case FIRST -> beginEpisode(player, persisted, episode, actionId);
            case REPEAT -> {
                // A repeated reflex is not evidence of a second learned control.
            }
            case DISTINCT_SECOND -> {
                persisted.remove(PERSISTED_KEY);
                player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
                MinecraftForge.EVENT_BUS.post(new CustomControlEpisodeEvent(
                        player, CustomControlEpisodeEvent.Kind.DISTINCT_SECOND, actionId, firstAction, token));
            }
        }
    }

    private static void beginEpisode(
            final ServerPlayer player,
            final CompoundTag persisted,
            final CompoundTag episode,
            final String actionId
    ) {
        final String token = player.getUUID() + ":custom-control:" + player.server.getTickCount();
        episode.putString(TOKEN_KEY, token);
        episode.putString(FIRST_ACTION_KEY, actionId);
        persisted.put(PERSISTED_KEY, episode);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        MinecraftForge.EVENT_BUS.post(new CustomControlEpisodeEvent(
                player, CustomControlEpisodeEvent.Kind.FIRST_ACCEPTED, actionId, actionId, token));
    }
}
