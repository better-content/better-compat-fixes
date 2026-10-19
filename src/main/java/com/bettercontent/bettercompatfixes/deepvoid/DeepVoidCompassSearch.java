package com.bettercontent.bettercompatfixes.deepvoid;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Bounded asynchronous metadata lookup. Never requests BIOMES, NOISE, FEATURES or FULL chunks. */
public final class DeepVoidCompassSearch {
    public static final int SEARCH_REGIONS = 8;
    private static final Map<MinecraftServer, Service> SERVICES = new WeakHashMap<>();
    private DeepVoidCompassSearch() { }

    public static boolean eligible(ItemStack stack, ResourceKey<Level> dimension, double y) {
        return dimension.equals(Level.OVERWORLD) && y < 0 && stack.is(Items.COMPASS) && !CompassItem.isLodestoneCompass(stack);
    }
    public static long horizontalDistanceSquared(BlockPos a, BlockPos b) {
        long x = (long) a.getX() - b.getX(), z = (long) a.getZ() - b.getZ();
        return x * x + z * z;
    }

    public static List<ChunkPos> candidates(long seed, BlockPos origin, RandomSpreadStructurePlacement placement) {
        int spacing = placement.spacing();
        int rx = Math.floorDiv(origin.getX() >> 4, spacing), rz = Math.floorDiv(origin.getZ() >> 4, spacing);
        List<ChunkPos> result = new ArrayList<>();
        for (int x = -SEARCH_REGIONS; x <= SEARCH_REGIONS; x++) for (int z = -SEARCH_REGIONS; z <= SEARCH_REGIONS; z++) {
            result.add(placement.getPotentialStructureChunk(seed, (rx + x) * spacing, (rz + z) * spacing));
        }
        result.sort(Comparator.comparingLong(pos -> horizontalDistanceSquared(origin,
                new BlockPos(pos.getMiddleBlockX(), 0, pos.getMiddleBlockZ()))));
        return result;
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 10 != 0) return;
        Service service = SERVICES.computeIfAbsent(server, ignored -> new Service());
        service.tick(server);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { SERVICES.remove(event.getServer()); }
    @SubscribeEvent public static void loggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Service service = SERVICES.get(player.server);
            if (service != null) service.jobs.remove(player.getUUID());
        }
    }
    @SubscribeEvent public static void reloaded(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            Service service = SERVICES.get(event.getPlayerList().getServer());
            if (service != null) {
                service.epoch++;
                service.jobs.clear();
                service.cache.clear();
                // Keep the in-flight slot reserved until its old request actually finishes.
                for (ServerPlayer player : event.getPlayerList().getPlayers()) DeepVoidCompassNetwork.send(player, player.blockPosition(), null);
            }
        }
    }

    public static java.util.concurrent.CompletableFuture<java.util.Optional<BlockPos>> requestStart(
            ServerLevel level, ChunkPos candidate, net.minecraft.world.level.levelgen.structure.Structure structure) {
        return level.getChunkSource().getChunkFuture(candidate.x, candidate.z, ChunkStatus.STRUCTURE_STARTS, true)
                .thenApply(result -> {
                    var chunk = result.left().orElseThrow(() -> new IllegalStateException("Structure metadata unavailable: " + candidate));
                    var start = chunk.getStartForStructure(structure);
                    return start != null && start.isValid()
                            ? java.util.Optional.of(new BlockPos(candidate.getMiddleBlockX(), level.getMinBuildHeight(), candidate.getMiddleBlockZ()))
                            : java.util.Optional.empty();
                });
    }

    private static final class Job {
        final BlockPos origin;
        final List<ChunkPos> candidates;
        final int created;
        int next;
        boolean complete;
        BlockPos target;
        Job(BlockPos origin, List<ChunkPos> candidates, int created) {
            this.origin = origin; this.candidates = candidates; this.created = created;
        }
    }
    private record Found(BlockPos target, int tick) { }
    private static final class Service {
        final Map<UUID, Job> jobs = new HashMap<>();
        final LinkedHashMap<Long, Found> cache = new LinkedHashMap<>();
        boolean inFlight;
        int cursor;
        int epoch;

        void tick(MinecraftServer server) {
            final ServerLevel level = server.overworld();
            final var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            final var holder = registry.getHolder(DeepVoidStructures.KEY);
            if (holder.isEmpty() || !server.getWorldData().worldGenOptions().generateStructures()) return;
            final var placements = level.getChunkSource().getGeneratorState().getPlacementsForStructure(holder.get());
            final var placement = placements.stream().filter(RandomSpreadStructurePlacement.class::isInstance)
                    .map(RandomSpreadStructurePlacement.class::cast).findFirst();
            if (placement.isEmpty()) return;
            final var players = server.getPlayerList().getPlayers();
            jobs.keySet().removeIf(id -> players.stream().noneMatch(player -> player.getUUID().equals(id)));
            for (ServerPlayer player : players) {
                boolean active = player.getInventory().items.stream().anyMatch(stack -> eligible(stack, player.level().dimension(), player.getY()))
                        || player.getInventory().offhand.stream().anyMatch(stack -> eligible(stack, player.level().dimension(), player.getY()));
                if (!active) {
                    if (jobs.remove(player.getUUID()) != null) DeepVoidCompassNetwork.send(player, player.blockPosition(), null);
                    continue;
                }
                Job job = jobs.get(player.getUUID());
                if (job == null || horizontalDistanceSquared(job.origin, player.blockPosition()) >= 32L * 32L
                        || server.getTickCount() - job.created >= 6000) {
                    var possible = candidates(level.getSeed(), player.blockPosition(), placement.get()).stream()
                            .filter(pos -> level.getWorldBorder().isWithinBounds(new BlockPos(pos.getMiddleBlockX(), 0, pos.getMiddleBlockZ())))
                            .toList();
                    job = new Job(player.blockPosition(), possible, server.getTickCount());
                    jobs.put(player.getUUID(), job);
                    DeepVoidCompassNetwork.send(player, job.origin, null);
                }
            }
            if (inFlight || players.isEmpty()) return;
            // Round-robin across players; one global in-flight request, at most one new request / 10 ticks.
            for (int i = 0; i < players.size(); i++) {
                ServerPlayer player = players.get(Math.floorMod(cursor++, players.size()));
                Job job = jobs.get(player.getUUID());
                if (job == null || job.complete) continue;
                for (int budget = 0; budget < 16 && job.next < job.candidates.size(); budget++) {
                    ChunkPos candidate = job.candidates.get(job.next++);
                    Found known = cache.get(candidate.toLong());
                    if (known != null && server.getTickCount() - known.tick < 1200) {
                        if (known.target != null) { finish(player, job, known.target); break; }
                        continue;
                    }
                    inFlight = true;
                    final int requestEpoch = epoch;
                    // STRUCTURE_STARTS creates/reads only structure metadata, not terrain. Old chunks with no
                    // entrance start are rejected rather than pointing at a fictional retroactive entrance.
                    requestStart(level, candidate, holder.get().value())
                            .whenComplete((result, failure) -> server.execute(() -> {
                                inFlight = false;
                                if (SERVICES.get(server) != this || requestEpoch != epoch) return;
                                if (failure != null) {
                                    LogUtils.getLogger().warn("Deep Void compass structure-start lookup failed at {}", candidate, failure);
                                    // Do not skip an unknown nearer entrance and advertise a farther one as nearest.
                                    if (jobs.get(player.getUUID()) == job && !player.hasDisconnected()) finish(player, job, null);
                                    return;
                                }
                                BlockPos target = result.orElse(null);
                                cache.put(candidate.toLong(), new Found(target, server.getTickCount()));
                                while (cache.size() > 512) cache.remove(cache.keySet().iterator().next());
                                if (jobs.get(player.getUUID()) == job && player.connection != null && !player.hasDisconnected()
                                        && target != null) finish(player, job, target);
                            }));
                    return;
                }
                if (!job.complete && job.next >= job.candidates.size()) finish(player, job, null);
            }
        }
        void finish(ServerPlayer player, Job job, BlockPos target) {
            job.target = target;
            job.complete = true;
            DeepVoidCompassNetwork.send(player, job.origin, target);
        }
    }
}
