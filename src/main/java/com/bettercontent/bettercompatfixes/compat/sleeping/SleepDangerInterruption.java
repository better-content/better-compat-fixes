package com.bettercontent.bettercompatfixes.compat.sleeping;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import github.cosmicdan.sleepingoverhaul.SleepingOverhaul;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Stops only Sleeping Overhaul's current timelapse when a sleeping player needs to respond. */
public final class SleepDangerInterruption {
    private static final double IMMEDIATE_THREAT_RANGE_SQUARED = 16.0D * 16.0D;

    private SleepDangerInterruption() {
    }

    public enum Reason {
        DAMAGE, HOSTILE_TARGET, DIRECTOR_WARNING, INJURY, FONT_WINDOW
    }

    /** Typed entry point for an owner that has admitted an immediate, player-actionable danger. */
    public static boolean interrupt(final ServerPlayer player, final Reason reason) {
        if (player == null || reason == null || !player.isSleeping()
                || !BcFixesConfig.sleepingOverhaulInterruptImmediateDanger()) return false;
        if (SleepingOverhaul.serverState == null || !SleepingOverhaul.serverState.isTimelapseActive()) return false;
        SleepingOverhaul.serverState.stopTimelapseNow(player.serverLevel());
        return true;
    }

    /** Called from Sleeping Overhaul's recursive server-tick path before another simulated tick. */
    public static boolean interruptIfImmediateDanger(final MinecraftServer server) {
        if (server == null || SleepingOverhaul.serverState == null || !SleepingOverhaul.serverState.isTimelapseActive()) return false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSleeping() && hasImmediateHostile(player)) return interrupt(player, Reason.HOSTILE_TARGET);
        }
        return false;
    }

    @SubscribeEvent
    public static void onSleepingPlayerHurt(final LivingHurtEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0F || !(event.getEntity() instanceof ServerPlayer player)) return;
        interrupt(player, Reason.DAMAGE);
    }

    @SubscribeEvent
    public static void onHostileTarget(final LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy)
                || !(event.getNewTarget() instanceof ServerPlayer player) || !hasImmediateHostile(mob, player)) return;
        interrupt(player, Reason.HOSTILE_TARGET);
    }

    static boolean shouldInterrupt(final boolean timelapseActive, final boolean immediateDanger) {
        return timelapseActive && immediateDanger;
    }

    private static boolean hasImmediateHostile(final ServerPlayer player) {
        return player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16.0D),
                mob -> hasImmediateHostile(mob, player)).stream().findFirst().isPresent();
    }

    private static boolean hasImmediateHostile(final Mob mob, final ServerPlayer player) {
        return mob.isAlive() && mob.getTarget() == player
                && mob.distanceToSqr(player) <= IMMEDIATE_THREAT_RANGE_SQUARED && mob.hasLineOfSight(player);
    }
}
