package com.bettercontent.bettercompatfixes.nativeforgeapifixture;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import net.mehvahdjukaar.moonlight.api.map.MapHelper;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicResourcePack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** Read-only original-operation witnesses. No dispatcher is registered/replaced/cancelled here. */
public final class ForgeApiNativeWitness {
    public static final ResourceLocation GENERATED_TAG = new ResourceLocation("supplementaries", "tags/items/sign_posts.json");
    private static final ResourceLocation GENERATED_PACK = new ResourceLocation("supplementaries", "generated_pack");
    public static final AtomicInteger ROOT_PACK_FINDERS = new AtomicInteger();
    public static final AtomicInteger RESOURCE_PACK_FINDERS = new AtomicInteger();
    private static final ThreadLocal<InteractionScope> INTERACTION = new ThreadLocal<>();
    private static volatile Generated generated;
    private static volatile Generated resolved;
    private static int generations;

    private ForgeApiNativeWitness() {}

    public static void packFinderCompleted(AddPackFindersEvent event, boolean root) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            (root ? ROOT_PACK_FINDERS : RESOURCE_PACK_FINDERS).incrementAndGet();
        }
    }

    public static void bridgeEnter(PlayerInteractEvent.RightClickBlock event) {
        var scope = INTERACTION.get();
        if (scope == null) return;
        require(scope.bridgeDepth == 0, "Nested/double original FFAPI Forge bridge");
        require(event.getEntity() == scope.operationPlayer && event.getLevel() == scope.operationPlayer.level()
                        && event.getHand() == scope.hand && event.getPos().equals(scope.hit.getBlockPos()),
                "Original FFAPI Forge bridge received wrong interaction identity");
        scope.entries++;
        scope.bridgeDepth++;
        scope.event = event;
        scope.preCancelled = event.isCanceled();
    }

    public static void bridgeReturn(PlayerInteractEvent.RightClickBlock event) {
        var scope = INTERACTION.get();
        if (scope == null) return;
        require(scope.bridgeDepth == 1 && event == scope.event, "Original FFAPI bridge did not complete its matching operation");
        scope.bridgeDepth--;
        scope.returns++;
        scope.cancelled = event.isCanceled();
        scope.cancellationResult = event.getCancellationResult();
    }

    /** Registered through the actual public FFAPI event API, never called directly by a test. */
    public static InteractionResult useBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        var scope = INTERACTION.get();
        if (scope == null) return InteractionResult.PASS;
        require(scope.bridgeDepth == 1 && scope.event != null,
                "FFAPI callback must originate inside the actual original Forge bridge");
        require(player == scope.operationPlayer && level == player.level() && hand == scope.hand
                        && hit.getBlockPos().equals(scope.hit.getBlockPos()), "FFAPI callback changed interaction identity");
        scope.callbacks++;
        boolean matches = player == scope.owner && hand == InteractionHand.MAIN_HAND
                && hit.getBlockPos().equals(scope.target);
        if (!matches) return InteractionResult.PASS;
        scope.matches++;
        if (scope.response == InteractionResult.FAIL) return InteractionResult.FAIL;
        require(scope.response == InteractionResult.PASS, "Unsupported fixture callback response");
        var map = MapHelper.getMapData(player.getItemInHand(hand), level, player);
        require(map != null, "Real held filled-map data must resolve through Moonlight MapHelper");
        var counter = NativeForgeApiBootstrap.counterType().get(map);
        require(counter != null, "Moonlight must create the registered CustomMapData for the actual map");
        counter.value++;
        counter.setDirty(map, NativeForgeApiBootstrap.Dirty::mark);
        map.setDirty();
        scope.mutations++;
        return InteractionResult.PASS;
    }

    public static InteractionScope interaction(ServerPlayer operationPlayer, ServerPlayer owner, BlockPos target,
            InteractionHand hand, BlockHitResult hit, InteractionResult response) {
        require(INTERACTION.get() == null, "Overlapping fixture interaction scopes");
        var scope = new InteractionScope(operationPlayer, owner, target, hand, hit, response);
        INTERACTION.set(scope);
        return scope;
    }

    public static final class InteractionScope implements AutoCloseable {
        final ServerPlayer operationPlayer;
        final ServerPlayer owner;
        final BlockPos target;
        final InteractionHand hand;
        final BlockHitResult hit;
        final InteractionResult response;
        int bridgeDepth;
        PlayerInteractEvent.RightClickBlock event;
        public int entries;
        public int returns;
        public int callbacks;
        public int matches;
        public int mutations;
        public boolean preCancelled;
        public boolean cancelled;
        public InteractionResult cancellationResult;

        private InteractionScope(ServerPlayer operationPlayer, ServerPlayer owner, BlockPos target,
                InteractionHand hand, BlockHitResult hit, InteractionResult response) {
            this.operationPlayer = operationPlayer;
            this.owner = owner;
            this.target = target.immutable();
            this.hand = hand;
            this.hit = hit;
            this.response = response;
        }

        public boolean completed() { return bridgeDepth == 0 && entries == 1 && returns == 1 && callbacks == 1; }

        @Override public void close() {
            require(INTERACTION.get() == this, "Fixture interaction scope ownership lost");
            INTERACTION.remove();
        }
    }

    /** Called only after original DynamicResourcePack.addBytes has actually committed the generated tag. */
    public static synchronized void generatedBytesCommitted(DynamicResourcePack pack, ResourceLocation location, byte[] input) {
        if (!GENERATED_PACK.equals(pack.id()) || !GENERATED_TAG.equals(location)) return;
        require(pack.getPackType() == PackType.SERVER_DATA, "Wrong native Moonlight generated pack type");
        var supplier = pack.getResource(PackType.SERVER_DATA, location);
        require(supplier != null, "Moonlight returned without committing its generated bytes");
        try (var stream = supplier.get()) {
            byte[] committed = stream.readAllBytes();
            require(Arrays.equals(input, committed), "Committed Moonlight resource differs from original generated content");
            generated = new Generated(++generations, pack.packId(), committed);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not read actual committed Moonlight generated resource", failure);
        }
    }

    /** Observe real manager resolution before Moonlight's legitimate post-reload memory cleanup. */
    public static void generatedResourceResolved(ResourceManager manager) {
        var committed = generated;
        require(committed != null, "Moonlight must generate real Supplementaries content before the FFAPI listener");
        var matches = manager.getResourceStack(GENERATED_TAG).stream()
                .filter(resource -> committed.packId().equals(resource.sourcePackId())).toList();
        require(matches.size() == 1, "Exactly one actual Moonlight dynamic-source tag must resolve during reload");
        try (var stream = matches.get(0).open()) {
            byte[] bytes = stream.readAllBytes();
            require(Arrays.equals(committed.bytes(), bytes), "Native ResourceManager resolved wrong Moonlight generated bytes");
            resolved = new Generated(committed.generation(), committed.packId(), bytes);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not resolve actual generated content during FFAPI reload", failure);
        }
    }

    public static Generated generated() { return generated; }
    public static Generated resolved() { return resolved; }

    public record Generated(int generation, String packId, byte[] bytes) {
        public Generated { bytes = bytes.clone(); }
        @Override public byte[] bytes() { return bytes.clone(); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
