package com.bettercontent.bettercompatfixes.api;

import com.bettercontent.bettercompatfixes.api.event.MobOffspringAdmissionEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.MinecraftForge;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Immediate causal scopes, including unowned actors which must mask an enclosing owned attacker. */
public final class CausalMobSpawns {
    private static final ActorScopes<Entity> ACTOR = new ActorScopes<>();
    private CausalMobSpawns() {}

    public static Entity actor() { return ACTOR.current(); }

    public static <T> T as(Entity actor, Supplier<T> action) { return ACTOR.call(actor, action); }

    public static void runAs(Entity actor, Runnable action) {
        as(actor, () -> { action.run(); return null; });
    }

    /** Wrap the real insertion result, not merely EntityJoinLevelEvent's preliminary admission. */
    public static boolean insert(Entity child, BooleanSupplier insertion) {
        Entity parent = actor();
        if (parent == null || parent == child || !(child instanceof Mob mob) || child.level().isClientSide()) {
            return insertion.getAsBoolean();
        }
        MobOffspringAdmissionEvent event = new MobOffspringAdmissionEvent(parent, mob);
        boolean inserted = false;
        try {
            if (!MinecraftForge.EVENT_BUS.post(event)) inserted = as(child, insertion::getAsBoolean);
            return inserted;
        } finally { event.complete(inserted); }
    }
}
