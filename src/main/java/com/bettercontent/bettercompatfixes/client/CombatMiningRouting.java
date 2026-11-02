package com.bettercontent.bettercompatfixes.client;

import com.bettercontent.bettercompatfixes.compat.TconCombatFirstPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.events.engine.RenderEngine;
import static com.bettercontent.bettercompatfixes.client.CombatMiningPolicy.*;

/** Client-only adapter. Always recompute: no stale state across swaps, targets or reloads. */
public final class CombatMiningRouting {
    private static final ResourceLocation BUTCHER_KNIFE =
            ResourceLocation.fromNamespaceAndPath("additionalweaponry", "butcher_knife");
    private CombatMiningRouting() {}

    public static Route current() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var patch = ClientEngine.getInstance().getPlayerPatch();
        if (player == null || minecraft.level == null || patch == null || !patch.isEpicFightMode())
            return Route.NATIVE;
        var stack = player.getMainHandItem();
        var kind = BUTCHER_KNIFE.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))
                ? ItemKind.BUTCHER_KNIFE
                : TconCombatFirstPolicy.deniesBlockAttack(player) ? ItemKind.COMBAT_FIRST : ItemKind.OTHER;
        if (kind == ItemKind.OTHER) return Route.NATIVE;

        var camera = EpicFightCameraAPI.getInstance();
        HitResult selected = minecraft.hitResult;
        var cameraHit = camera.getCrosshairHitResult();
        double reach = player.getBlockReach();
        // Exact EF 20.14.17 selection: strict near-camera test, otherwise vanilla fallback.
        if (camera.isTPSMode() && cameraHit != null
                && cameraHit.getLocation().distanceToSqr(player.getEyePosition()) < reach * reach)
            selected = cameraHit;
        var entityHit = selected == null ? null : RenderEngine.asEntityHitResult(selected);
        var blockHit = selected == null ? null : RenderEngine.asBlockHitResult(selected);
        Target target = entityHit != null
                ? (entityHit.getEntity() instanceof LivingEntity || entityHit.getEntity() instanceof PartEntity<?>
                    ? Target.LIVING : Target.NONLIVING)
                : blockHit != null ? Target.BLOCK : Target.NONE;
        // A null target precedes lock-on in EF and remains a combat attempt for affected items.
        boolean locked = selected != null && camera.isLockingOnTarget();
        boolean matches = false;
        boolean mineable = false;
        if (kind == ItemKind.BUTCHER_KNIFE && target == Target.BLOCK && !locked) {
            var raw = minecraft.hitResult == null ? null : RenderEngine.asBlockHitResult(minecraft.hitResult);
            matches = raw != null && raw.getBlockPos().equals(blockHit.getBlockPos());
            if (matches && raw.getLocation().distanceToSqr(player.getEyePosition()) <= reach * reach) {
                var pos = raw.getBlockPos();
                var state = minecraft.level.getBlockState(pos);
                mineable = !state.isAir() && state.getDestroySpeed(minecraft.level, pos) >= 0
                        && stack.getItem().canAttackBlock(state, minecraft.level, pos, player);
            }
        }
        return decide(true, true, kind, target, locked, matches, mineable);
    }
}
