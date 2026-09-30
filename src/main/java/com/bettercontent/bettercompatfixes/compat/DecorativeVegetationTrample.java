package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Removes only vanilla decorative grass while a living entity physically moves through it. */
public final class DecorativeVegetationTrample {
    private static final double MINIMUM_HORIZONTAL_MOVEMENT_SQUARED = 1.0E-6D;
    private static final Map<LivingEntity, BlockPos> LAST_OCCUPIED_BLOCK =
            Collections.synchronizedMap(new WeakHashMap<>());

    private DecorativeVegetationTrample() {
    }

    @SubscribeEvent
    public static void onLivingTick(final LivingEvent.LivingTickEvent event) {
        final LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        final BlockPos position = entity.blockPosition();
        final BlockPos previous = LAST_OCCUPIED_BLOCK.put(entity, position.immutable());
        trampleIfEntering(
                entity.level(),
                position,
                entity.getDeltaMovement(),
                previous == null || !previous.equals(position),
                BcFixesConfig.vegetationDecorativeTrampleChance(),
                entity.getRandom().nextDouble());
    }

    public static boolean trampleIfEntering(
            final Level level,
            final BlockPos pos,
            final Vec3 movement,
            final boolean distinctEntry,
            final double chance,
            final double roll
    ) {
        if (level.isClientSide || !distinctEntry
                || movement.horizontalDistanceSqr() <= MINIMUM_HORIZONTAL_MOVEMENT_SQUARED
                || roll >= chance) {
            return false;
        }
        if (!isTrampleable(level.getBlockState(pos))) {
            return false;
        }
        return level.destroyBlock(pos, false);
    }

    static boolean isTrampleable(final BlockState state) {
        return state.is(Blocks.GRASS)
                || state.is(Blocks.TALL_GRASS);
    }
}
