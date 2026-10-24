package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.moonlightplatform.NativeForgeApiPlatform;
import com.bettercontent.bettercompatfixes.nativeforgeapifixture.ForgeApiNativeWitness;
import com.bettercontent.bettercompatfixes.nativeforgeapifixture.NativeForgeApiBootstrap;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Two additive real-operation cases; no direct event invoker or replacement dispatcher is used. */
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MoonlightNativeForgeGameTests {
    private MoonlightNativeForgeGameTests() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(MoonlightNativeForgeGameTests.class);
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "native_forge_api")
    public static void nativeForgeInteractionPersistsMoonlightMapData(GameTestHelper helper) {
        assertNativePair(helper);
        var level = helper.getLevel();
        var target = helper.absolutePos(new BlockPos(1, 1, 1));
        var other = helper.absolutePos(new BlockPos(2, 1, 1));
        level.setBlock(target.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(other.below(), Blocks.STONE.defaultBlockState(), 3);
        var lever = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR);
        level.setBlock(target, lever, 3);
        level.setBlock(other, lever, 3);
        var owner = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "native_api_owner"));
        var outsider = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "native_api_other"));
        owner.setGameMode(GameType.SURVIVAL);
        outsider.setGameMode(GameType.SURVIVAL);
        owner.moveTo(Vec3.atCenterOf(target).add(0, 0, 2));
        outsider.moveTo(Vec3.atCenterOf(target).add(0, 0, 2));
        ItemStack held = MapItem.create(level, target.getX(), target.getZ(), (byte) 0, true, false);
        owner.setItemInHand(InteractionHand.MAIN_HAND, held);
        owner.setItemInHand(InteractionHand.OFF_HAND, held.copy());
        outsider.setItemInHand(InteractionHand.MAIN_HAND, held.copy());
        var map = MapItem.getSavedData(held, level);
        helper.assertTrue(map != null, "A real filled-map stack must own actual saved map data");
        var counter = NativeForgeApiBootstrap.counterType().get(map);
        helper.assertTrue(counter != null && counter.value == 0,
                "Moonlight constructor injection must create the pre-registered custom payload without test insertion");
        map.colors[0] = 29;

        for (int repeat = 1; repeat <= 2; repeat++) {
            boolean powered = level.getBlockState(target).getValue(LeverBlock.POWERED);
            var result = interact(helper, owner, owner, target, target, InteractionHand.MAIN_HAND, InteractionResult.PASS, 1, 1);
            helper.assertTrue(result.consumesAction(), "PASS callback must allow original native lever operation");
            helper.assertTrue(level.getBlockState(target).getValue(LeverBlock.POWERED) != powered,
                    "Original world interaction must really toggle the lever on every permitted operation");
            helper.assertTrue(counter.value == repeat && map.isDirty(), "Exactly one actual Moonlight mutation required per operation");
            assertMapRoundTrip(helper, map, repeat);
        }
        boolean powered = level.getBlockState(target).getValue(LeverBlock.POWERED);
        var denied = interact(helper, owner, owner, target, target, InteractionHand.MAIN_HAND, InteractionResult.FAIL, 1, 0);
        helper.assertTrue(denied == InteractionResult.FAIL && level.getBlockState(target).getValue(LeverBlock.POWERED) == powered,
                "Real FAIL callback must propagate through the original Forge event and prevent original lever use");

        // Each nonmatching operation must still traverse the real bridge and perform its original vanilla operation.
        for (int control = 0; control < 3; control++) {
            var player = control == 2 ? outsider : owner;
            var position = control == 0 ? other : target;
            var hand = control == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            boolean before = level.getBlockState(position).getValue(LeverBlock.POWERED);
            var result = interact(helper, player, owner, target, position, hand, InteractionResult.PASS, 0, 0);
            helper.assertTrue(result.consumesAction() && level.getBlockState(position).getValue(LeverBlock.POWERED) != before,
                    "Nonmatching position/hand/player must retain the original native lever operation");
            helper.assertTrue(counter.value == 2, "Nonmatching or denied operations must not mutate Moonlight data");
        }
        assertMapRoundTrip(helper, map, 2);
        helper.succeed();
    }

    private static InteractionResult interact(GameTestHelper helper, ServerPlayer player, ServerPlayer owner,
            BlockPos target, BlockPos position, InteractionHand hand, InteractionResult response, int matches, int mutations) {
        var hit = new BlockHitResult(Vec3.atCenterOf(position), Direction.UP, position, false);
        try (var scope = ForgeApiNativeWitness.interaction(player, owner, target, hand, hit, response)) {
            // This is the real server entrypoint: ForgeHooks -> registered FFAPI subscriber -> public API callback.
            var result = player.gameMode.useItemOn(player, helper.getLevel(), player.getItemInHand(hand), hand, hit);
            helper.assertTrue(scope.completed(), "One complete original Forge→FFAPI bridge and one actual callback required");
            helper.assertTrue(!scope.preCancelled && scope.matches == matches && scope.mutations == mutations,
                    "Wrong native callback scope, mutation count, or earlier cancellation");
            boolean denied = matches == 1 && response == InteractionResult.FAIL;
            helper.assertTrue(scope.cancelled == denied && (!denied || scope.cancellationResult == InteractionResult.FAIL),
                    "Actual FFAPI bridge must preserve PASS/FAIL Forge event semantics");
            return result;
        }
    }

    private static void assertMapRoundTrip(GameTestHelper helper, MapItemSavedData map, int expected) {
        // Never call Counter.save/load directly: only Moonlight's actual map save/load injections may do so.
        CompoundTag encoded = map.save(new CompoundTag());
        helper.assertTrue(encoded.getCompound(NativeForgeApiBootstrap.COUNTER_NBT).getInt("value") == expected,
                "Moonlight must actually serialize its custom data through vanilla map save");
        var loaded = MapItemSavedData.load(encoded.copy());
        var original = NativeForgeApiBootstrap.counterType().get(map);
        var restored = NativeForgeApiBootstrap.counterType().get(loaded);
        helper.assertTrue(loaded != map && restored != null && restored != original && restored.value == expected,
                "Moonlight must construct and load an independent custom payload during real map deserialization");
        helper.assertTrue(loaded.colors[0] == 29 && loaded.scale == map.scale && loaded.dimension.equals(map.dimension),
                "Original vanilla map color/scale/dimension persistence must remain intact");
        CompoundTag withoutPayload = encoded.copy();
        withoutPayload.remove(NativeForgeApiBootstrap.COUNTER_NBT);
        var zero = NativeForgeApiBootstrap.counterType().get(MapItemSavedData.load(withoutPayload));
        helper.assertTrue(zero != null && zero.value == 0, "Missing-payload negative control must not retain prior custom values");
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "native_forge_api", timeoutTicks = 2400)
    public static void nativeForgeResourceReloadRegeneratesMoonlightData(GameTestHelper helper) {
        assertNativePair(helper);
        var server = helper.getLevel().getServer();
        assertNativePackDiscovery(helper, server);
        assertGeneratedResolution(helper);
        var run = new ReloadRun(helper, server);
        helper.onEachTick(run);
    }

    private static void assertNativePair(GameTestHelper helper) {
        var mods = ModList.get();
        helper.assertTrue(mods.isLoaded("fabric_api") && mods.isLoaded("moonlight") && mods.isLoaded("supplementaries"),
                "Actual native profile must retain installed FFAPI identity and both Moonlight/dependent providers");
        helper.assertTrue(!NativeForgeApiPlatform.hasUnsupportedFabricApiProvider(mods, "fabric_api"),
                "Actual current Forge containers/owners/archives/ABIs must satisfy the production supported-pair predicate");
        helper.assertTrue(mods.isLoaded("fabric_api"), "Scoped Moonlight predicate must not falsify global mod presence");
        NativeForgeApiBootstrap.counterType();
    }

    private static void assertNativePackDiscovery(GameTestHelper helper, MinecraftServer server) {
        helper.assertTrue(ForgeApiNativeWitness.ROOT_PACK_FINDERS.get() > 0 && ForgeApiNativeWitness.RESOURCE_PACK_FINDERS.get() > 0,
                "Both actual FFAPI Forge AddPackFindersEvent handlers must have completed naturally");
        helper.assertTrue(server.getPackRepository().getSelectedIds().contains("fabric"),
                "Native FFAPI's real mandatory pack must be selected, not merely an existing class");
        var pack = server.getPackRepository().getPack("fabric");
        helper.assertTrue(pack != null, "Native FFAPI repository source must discover its actual pack");
        try (var resources = pack.open()) {
            var supplier = resources.getRootResource("pack.mcmeta");
            helper.assertTrue(supplier != null, "Native FFAPI discovered pack must expose its actual descriptor");
            try (var stream = supplier.get()) {
                String descriptor = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                var json = JsonParser.parseString(descriptor).getAsJsonObject().getAsJsonObject("pack");
                helper.assertTrue(json.get("pack_format").getAsInt() == 15
                                && json.getAsJsonObject("description").get("text").getAsString().equals("fabric_api dummy resources"),
                        "Wrong native FFAPI discovered resource-pack bytes");
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Actual FFAPI native pack cannot be opened", failure);
        }
    }

    private static void assertGeneratedResolution(GameTestHelper helper) {
        var committed = ForgeApiNativeWitness.generated();
        var resolved = ForgeApiNativeWitness.resolved();
        helper.assertTrue(NativeForgeApiBootstrap.RELOADS.get() > 0 && NativeForgeApiBootstrap.inputPack != null,
                "Actual registered FFAPI listener must read its real fixture resource on initial load/reload");
        helper.assertTrue(committed != null && resolved != null && committed.generation() == resolved.generation()
                        && committed.packId().equals(resolved.packId()) && Arrays.equals(committed.bytes(), resolved.bytes()),
                "FFAPI listener must resolve exactly the content committed by the real Moonlight generation");
        var values = JsonParser.parseString(new String(resolved.bytes(), StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonArray("values");
        helper.assertTrue(values != null && values.asList().stream().anyMatch(v -> v.isJsonPrimitive()
                        && v.getAsString().equals("supplementaries:sign_post_oak"))
                        && values.asList().stream().anyMatch(v -> v.isJsonPrimitive()
                        && v.getAsString().equals("supplementaries:sign_post_spruce")),
                "Resolved Moonlight-generated tag must contain actual known native sign-post items");
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("supplementaries", "sign_post_oak"));
        var key = TagKey.create(Registries.ITEM, new ResourceLocation("supplementaries", "sign_posts"));
        helper.assertTrue(item != null && item.builtInRegistryHolder().is(key),
                "Actual tag reload consumer must bind the known Supplementaries item after natural generation");
    }

    private static final class ReloadRun implements Runnable {
        private final GameTestHelper helper;
        private final MinecraftServer server;
        private CompletableFuture<Void> pending;
        private int reloadsBefore;
        private int generationBefore;
        private int completedRounds;
        private boolean done;

        ReloadRun(GameTestHelper helper, MinecraftServer server) {
            this.helper = helper;
            this.server = server;
        }

        @Override public void run() {
            if (done) return;
            if (pending == null) {
                reloadsBefore = NativeForgeApiBootstrap.RELOADS.get();
                generationBefore = ForgeApiNativeWitness.generated().generation();
                List<String> selected = List.copyOf(server.getPackRepository().getSelectedIds());
                // Native MinecraftServer.reloadResources internally managedBlocks when called on-server-thread.
                // Call that supported API off-thread; it schedules its own state changes on the real server.
                pending = CompletableFuture.supplyAsync(() -> {
                    if (server.isSameThread()) throw new IllegalStateException("Fixture must not block the server thread for reload");
                    return server.reloadResources(selected);
                }, Util.backgroundExecutor()).thenCompose(future -> future);
                return;
            }
            if (!pending.isDone()) return;
            try {
                pending.join(); // Already completed: never waits/blocks a pending native operation.
            } catch (RuntimeException failure) {
                done = true;
                throw new IllegalStateException("Natural asynchronous native resource reload failed", failure);
            }
            helper.assertTrue(NativeForgeApiBootstrap.RELOADS.get() == reloadsBefore + 1,
                    "Exactly one original registered FFAPI listener delivery required per real reload");
            helper.assertTrue(ForgeApiNativeWitness.generated().generation() == generationBefore + 1,
                    "Each real reload must actually regenerate/commit Moonlight content, not reuse an old INFO/snapshot");
            assertGeneratedResolution(helper);
            assertNativePackDiscovery(helper, server);
            if (++completedRounds == 2) {
                done = true;
                helper.succeed();
            } else {
                pending = null;
            }
        }
    }
}
