package com.bettercontent.bettercompatfixes.compat.epicfight;

import com.bettercontent.gameplaynotices.GameplayNotice;
import com.bettercontent.gameplaynotices.GameplayNotices;
import com.bettercontent.gameplaynotices.NoticeTheme;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/** Unlocks the held weapon's native basic combo when an Epic Fight basic attack begins. */
public final class StyleDiscovery {
    private StyleDiscovery() {}

    public static void observeAttackStart(ServerPlayerPatch patch) {
        ServerPlayer player = patch.getOriginal();
        if (player == null || player.level().isClientSide) return;
        InteractionHand hand = patch.getAttackingHand() == null
                ? InteractionHand.MAIN_HAND : patch.getAttackingHand();
        ItemStack held = player.getItemInHand(hand).copy();
        if (held.isEmpty()) return;
        var capability = patch.getHoldingItemCapability(hand);
        if (capability == null || capability.isEmpty()) return;
        var entry = StyleCatalogue.observe(patch, held, capability);
        if (entry == null || !StyleState.learn(player, patch, entry)) return;

        GameplayNotices.send(player, new GameplayNotice("style:" + entry.id(), NoticeTheme.COMBAT,
                Component.translatable("message.better_compat_fixes.style_discovered", entry.name()),
                Component.translatable("message.better_compat_fixes.style_hint")));
        StyleNetwork.sync(player, patch);
    }
}
