package com.bettercontent.bettercompatfixes.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.bettercontent.bettercompatfixes.compat.epicfight.StyleDiscovery;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.UUID;

@Mixin(value = ServerPlayerPatch.class, remap = false)
public abstract class ServerPlayerPatchMixin {
    private static final String BETTER_CONTENT_STYLE_DISCOVERY_LISTENER_ID =
            "f43c673d-8b1b-40c7-9e99-3234ac14d222";

    @Inject(method = "onJoinWorld(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraftforge/event/entity/EntityJoinLevelEvent;)V",
            at = @At("TAIL"), remap = false, require = 1)
    private void betterContentFixes$registerStyleDiscovery(final ServerPlayer player,
            final EntityJoinLevelEvent event, final CallbackInfo ci) {
        final ServerPlayerPatch patch = (ServerPlayerPatch) (Object) this;
        final PlayerEventListener listener = patch.getEventListener();
        final UUID listenerId = UUID.fromString(BETTER_CONTENT_STYLE_DISCOVERY_LISTENER_ID);
        listener.removeListener(PlayerEventListener.EventType.BASIC_ATTACK_EVENT,
                listenerId);
        listener.addEventListener(PlayerEventListener.EventType.BASIC_ATTACK_EVENT,
                listenerId,
                attack -> StyleDiscovery.observeAttackStart(
                        (ServerPlayerPatch) attack.getPlayerPatch()));
    }

    @Inject(method = "toVanillaMode", at = @At("HEAD"), cancellable = true, require = 1)
    private void betterContentFixes$keepBattleMode(final boolean synchronize, final CallbackInfo ci) {
        ((ServerPlayerPatch) (Object) this).toEpicFightMode(synchronize);
        ci.cancel();
    }
}
