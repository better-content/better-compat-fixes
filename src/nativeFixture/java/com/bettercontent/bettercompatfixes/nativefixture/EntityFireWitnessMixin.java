package com.bettercontent.bettercompatfixes.nativefixture;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Source-only fire-origin observation; no mutation, suppression or production attachment. */
@Mixin(value = Entity.class, remap = false)
public abstract class EntityFireWitnessMixin {
    @Inject(method = {"setSecondsOnFire(I)V", "m_20254_(I)V"}, at = @At("HEAD"),
            require = 1, remap = false)
    private void bcWitnessFireSeconds(int seconds, CallbackInfo callback) {
        bcWitnessFire("seconds", seconds);
    }

    @Inject(method = {"setRemainingFireTicks(I)V", "m_20268_(I)V"}, at = @At("HEAD"),
            require = 1, remap = false)
    private void bcWitnessFireTicks(int ticks, CallbackInfo callback) {
        if (ticks > 0) bcWitnessFire("ticks", ticks);
    }

    private void bcWitnessFire(String unit, int amount) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Skeleton) && !(entity instanceof Zombie)) return;
        LogUtils.getLogger().info("NATIVE_FIRE_ORIGIN uuid={} type={} pos={} game_tick={} entity_tick={} {}={} stack={}",
                entity.getUUID(), entity.getType(), entity.position(), entity.level().getGameTime(),
                entity.tickCount, unit, amount, Arrays.toString(new Throwable().getStackTrace()));
    }
}
