package com.bettercontent.bettercompatfixes.compat.epicfight;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

public final class StyleEvents {
    private StyleEvents() {}

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var patch = EpicFightCapabilities.getServerPlayerPatch(player);
            if (patch != null) StyleNetwork.sync(player, patch);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        CompoundTag old = event.getOriginal().getPersistentData()
                .getCompound(Player.PERSISTED_NBT_TAG);
        if (!old.isEmpty()) event.getEntity().getPersistentData()
                .put(Player.PERSISTED_NBT_TAG, old.copy());
    }

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent event) {
        StyleCatalogue.invalidate();
    }
}
