package com.bettercontent.bettercompatfixes.compat.epicfight;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/** Changes only basic combo selection and collision resolution, never the held capability. */
public final class StyleCombat {
    private StyleCombat() {}

    public static List<AnimationAccessor<? extends AttackAnimation>> combo(
            CapabilityItem capability, PlayerPatch<?> patch) {
        var nativeCombo = capability.getAutoAttackMotion(patch);
        if (!(patch.getOriginal() instanceof ServerPlayer player)
                || !StyleCatalogue.melee(capability)) return nativeCombo;
        String id = StyleState.selected(player, (yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch) patch);
        if (id.isEmpty()) return nativeCombo;
        var entry = StyleCatalogue.get(patch, id);
        if (entry == null) return nativeCombo;
        var alternate = new ArrayList<AnimationAccessor<? extends AttackAnimation>>();
        for (var key : entry.animations()) {
            AnimationAccessor<? extends AttackAnimation> accessor = AnimationManager.byKey(key);
            if (accessor == null || !(accessor.get() instanceof AttackAnimation)) return nativeCombo;
            alternate.add(accessor);
        }
        return alternate.size() >= 3 ? List.copyOf(alternate) : nativeCombo;
    }

    public static List<Entity> collide(Collider authored, LivingEntityPatch<?> patch,
            AttackAnimation animation, float prev, float current, Joint joint, float partial) {
        Collider used = authored;
        if (patch instanceof PlayerPatch<?> playerPatch
                && patch.getOriginal() instanceof ServerPlayer player) {
            var serverPatch = (yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch) playerPatch;
            String id = StyleState.selected(player, serverPatch);
            var entry = id.isEmpty() ? null : StyleCatalogue.get(playerPatch, id);
            if (entry != null && entry.animations().contains(animation.getRegistryName())) {
                Collider held = patch.getColliderMatching(InteractionHand.MAIN_HAND);
                if (held != null) used = held;
            }
        }
        return used.updateAndSelectCollideEntity(patch, animation, prev, current, joint, partial);
    }
}
