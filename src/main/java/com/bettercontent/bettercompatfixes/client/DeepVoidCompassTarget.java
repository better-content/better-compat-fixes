package com.bettercontent.bettercompatfixes.client;

import com.bettercontent.bettercompatfixes.deepvoid.DeepVoidCompassSearch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Client-only, transient per-connection state. No compass NBT or lodestone target is changed. */
public final class DeepVoidCompassTarget {
    private static java.lang.ref.WeakReference<ClientLevel> owner = new java.lang.ref.WeakReference<>(null);
    private static BlockPos origin;
    private static BlockPos target;
    private static long received;
    private DeepVoidCompassTarget() { }

    public static void accept(BlockPos from, BlockPos to) {
        ClientLevel level = Minecraft.getInstance().level;
        owner = new java.lang.ref.WeakReference<>(level);
        origin = from;
        target = to;
        received = level == null ? 0 : level.getGameTime();
    }

    public static GlobalPos select(GlobalPos vanilla, ClientLevel level, ItemStack stack, Entity entity) {
        if (owner.get() != level || target == null || origin == null || entity != Minecraft.getInstance().player
                || !DeepVoidCompassSearch.eligible(stack, level.dimension(), entity.getY())
                || level.getGameTime() - received < 0 || level.getGameTime() - received > 6000
                || DeepVoidCompassSearch.horizontalDistanceSquared(origin, entity.blockPosition()) > 64L * 64L) return vanilla;
        return GlobalPos.of(Level.OVERWORLD, target);
    }
}
