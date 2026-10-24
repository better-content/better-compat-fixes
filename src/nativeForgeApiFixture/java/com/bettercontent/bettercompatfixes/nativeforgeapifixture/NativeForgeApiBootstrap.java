package com.bettercontent.bettercompatfixes.nativeforgeapifixture;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.mehvahdjukaar.moonlight.api.map.CustomMapData;
import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Optional source-set bootstrap, never present in the shipping mod or the five established profiles. */
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NativeForgeApiBootstrap {
    public static final ResourceLocation COUNTER_ID = id("native_forge_api_counter");
    public static final ResourceLocation INPUT = id("native_forge_api/probe.json");
    public static final String INPUT_TEXT = "{\"fixture\":\"native-forge-api\",\"revision\":1}\n";
    public static final String COUNTER_NBT = "better_content_native_forge_api";
    public static final AtomicInteger RELOADS = new AtomicInteger();
    public static volatile String inputPack;
    private static CustomMapData.Type<Counter> counterType;
    private static boolean registered;

    private NativeForgeApiBootstrap() {}

    public static ResourceLocation id(String path) {
        return new ResourceLocation(BetterContentFixes.MOD_ID, path);
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        // Both registries must be established before any server maps or initial data reload exist.
        event.enqueueWork(() -> {
            if (registered) throw new IllegalStateException("Native Forge API fixture bootstrap registered twice");
            counterType = MapDataRegistry.registerCustomMapSavedData(COUNTER_ID, Counter::new);
            UseBlockCallback.EVENT.register(ForgeApiNativeWitness::useBlock);
            ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new ProbeReloadListener());
            registered = true;
        });
    }

    public static CustomMapData.Type<Counter> counterType() {
        if (!registered || counterType == null) throw new IllegalStateException("Native Forge API fixture bootstrap missing");
        return counterType;
    }

    private static final class ProbeReloadListener implements SimpleSynchronousResourceReloadListener {
        @Override public ResourceLocation getFabricId() {
            return id("native_forge_api_reload");
        }

        @Override public void onResourceManagerReload(ResourceManager manager) {
            var resource = manager.getResource(INPUT).orElseThrow(() ->
                    new IllegalStateException("Actual FFAPI reload listener cannot resolve " + INPUT));
            try (var stream = resource.open()) {
                String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                if (!INPUT_TEXT.equals(text)) throw new IllegalStateException("Actual FFAPI listener read wrong fixture bytes");
                ForgeApiNativeWitness.generatedResourceResolved(manager);
                inputPack = resource.sourcePackId();
                RELOADS.incrementAndGet();
            } catch (IOException failure) {
                throw new IllegalStateException("Actual FFAPI reload listener read failed", failure);
            }
        }
    }

    /** Payload is deliberately not inserted/loaded by the tests: Moonlight must construct and persist it. */
    public static final class Counter implements CustomMapData<Dirty> {
        public int value;
        @Override public Type<?> getType() { return counterType(); }
        @Override public Dirty createDirtyCounter() { return new Dirty(); }
        @Override public void load(CompoundTag root) { value = root.getCompound(COUNTER_NBT).getInt("value"); }
        @Override public void loadUpdateTag(CompoundTag root) { load(root); }
        @Override public void save(CompoundTag root) {
            var payload = new CompoundTag();
            payload.putInt("value", value);
            root.put(COUNTER_NBT, payload);
        }
        @Override public void saveToUpdateTag(CompoundTag root, Dirty dirty) { save(root); }
    }

    public static final class Dirty implements CustomMapData.DirtyCounter {
        private boolean dirty;
        public void mark() { dirty = true; }
        @Override public boolean isDirty() { return dirty; }
        @Override public void clearDirty() { dirty = false; }
    }
}
