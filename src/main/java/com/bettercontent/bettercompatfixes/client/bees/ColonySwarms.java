package com.bettercontent.bettercompatfixes.client.bees;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.bees.ColonyActivity;
import com.bettercontent.bettercompatfixes.compat.bees.FairColonyScan;
import com.bettercontent.bettercompatfixes.compat.bees.DecodedColorScope;
import com.bettercontent.bettercompatfixes.compat.bees.TintedBeeOptions;
import com.bettercontent.bettercompatfixes.config.BcFixesClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, value = Dist.CLIENT)
public final class ColonySwarms {
    public static final int RANGE = 24, MAX_ALIVE = 128, MAX_BURST = 8, SCAN_LIMIT = 512;
    private static final ArrayDeque<Long> expiry = new ArrayDeque<>();
    private static final FairColonyScan<Long, BlockEntity> scan = new FairColonyScan<>();
    private static ClientLevel lastLevel;
    private static long emissions;
    private static long ticks;
    private ColonySwarms() {}

    /** Emission requests only; engine acceptance/rendering requires native observation. */
    public static long emittedCount() { return emissions; }

    public static boolean occupied(BlockEntity entity) {
        return ModList.get().isLoaded("complicated_bees") && NativeColonies.occupied(entity);
    }

    /** Primitive native body appearance only; unknown never falls back to invented yellow/nest tint. */
    public static DecodedColorScope.Sample bodyColor(BlockEntity entity) {
        return ModList.get().isLoaded("complicated_bees") ? NativeColonies.bodyColor(entity) : DecodedColorScope.Sample.UNKNOWN;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.isPaused()) return;
        if (lastLevel != mc.level) { expiry.clear(); scan.clear(); ticks = 0; lastLevel = mc.level; }
        if (mc.level == null || mc.player == null || !ModList.get().isLoaded("complicated_bees")) return;
        long time = ++ticks; // native unpaused client ticks, independent of server clock changes
        int density = BcFixesClientConfig.beeSwarmDensity();
        ParticleStatus particles = mc.options.particles().get();
        if (density == 0 || particles == ParticleStatus.MINIMAL) { scan.clear(); return; }
        while (!expiry.isEmpty() && expiry.peekFirst() <= time) expiry.removeFirst();
        if (time % 10 != 0) return;
        int budget = Math.min(MAX_BURST, density * (particles == ParticleStatus.DECREASED ? 1 : 2));
        BlockPos center = mc.player.blockPosition();
        Map<Long, Iterable<BlockEntity>> loaded = new LinkedHashMap<>();
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            LevelChunk chunk = mc.level.getChunkSource().getChunk((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz, false);
            if (chunk != null) loaded.put(chunk.getPos().toLong(), chunk.getBlockEntities().values());
        }
        List<BlockEntity> sources = scan.scan(loaded, SCAN_LIMIT,
                entity -> !entity.isRemoved() && entity.getBlockPos().distToCenterSqr(mc.player.position()) <= RANGE * RANGE && occupied(entity),
                Math.min(budget, MAX_ALIVE - expiry.size()));
        for (BlockEntity entity : sources) {
            DecodedColorScope.Sample appearance = bodyColor(entity);
            if (!appearance.known()) continue;
            BlockPos pos = entity.getBlockPos();
            mc.level.addParticle(new TintedBeeOptions(appearance.rgb()), pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0, 0);
            expiry.addLast(time + ColonyBeeParticle.LIFETIME + 1);
            emissions++;
        }
    }

    /** Loaded only after optional-mod admission; absence never links native bee classes. */
    private static final class NativeColonies {
        static DecodedColorScope.Sample bodyColor(BlockEntity entity) {
            if (entity instanceof com.accbdd.complicated_bees.block.entity.BeeNestBlockEntity nest)
                return com.bettercontent.bettercompatfixes.compat.bees.NativeBeeColors.fromSpecies(nest.getSpecies());
            if (entity instanceof com.accbdd.complicated_bees.block.entity.ApiaryBlockEntity && entity instanceof ColonyActivity activity
                    && activity.betterCompat$colonyActive() && activity.betterCompat$colonyColorKnown())
                return new DecodedColorScope.Sample(true, activity.betterCompat$colonyColor());
            return DecodedColorScope.Sample.UNKNOWN;
        }
        static boolean occupied(BlockEntity entity) {
            if (entity instanceof com.accbdd.complicated_bees.block.entity.BeeNestBlockEntity nest) {
                return nest.getSpecies() != com.accbdd.complicated_bees.bees.Species.INVALID;
            }
            return entity instanceof com.accbdd.complicated_bees.block.entity.ApiaryBlockEntity
                    && entity instanceof ColonyActivity activity && activity.betterCompat$colonyActive();
        }
    }
}
