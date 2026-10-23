package com.bettercontent.bettercompatfixes.nativewitness;

import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.worldgen.GenerationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Default-off gameTestServer-only observer, excluded from every shipping/sources archive. */
@Mod.EventBusSubscriber(modid = "better_compat_fixes")
public final class RootySoilWitness {
    public static final String PROPERTY = "better_compat_fixes.nativeRootySoilWitness";
    private static final boolean ENABLED = Boolean.getBoolean(PROPERTY) && Boolean.getBoolean("forge.gameTestServer");
    private static final RootySoilLedger LEDGER = ENABLED ? new RootySoilLedger() : null;
    private static final ThreadLocal<RootySoilLedger.Generation> GENERATION = new ThreadLocal<>();
    private RootySoilWitness() { }

    public static boolean enabled() { return ENABLED; }
    private static <T> T observe(Supplier<T> action, T fallback) {
        try { return action.get(); }
        catch (RuntimeException | Error observationFailure) { LEDGER.observerFailure(); return fallback; }
    }
    public static RootySoilLedger.Call begin() {
        return ENABLED ? observe(() -> LEDGER.begin(GENERATION.get()), null) : null;
    }
    public static void finish(RootySoilLedger.Call call, Boolean result, String exceptionClass) {
        if (ENABLED) observe(() -> { LEDGER.finish(call, result, exceptionClass); return null; }, null);
    }
    public static RootySoilLedger.Generation enter(GenerationContext context) {
        if (!ENABLED) return null;
        RootySoilLedger.Generation previous = GENERATION.get();
        observe(() -> {
            GENERATION.set(new RootySoilLedger.Generation(pos(context.originPos()), pos(context.rootPos()),
                    context.radius(), context.facing().get3DDataValue(), context.secondChanceRegen()));
            return null;
        }, null);
        return previous;
    }
    public static void leave(RootySoilLedger.Generation previous) {
        if (!ENABLED) return;
        if (previous == null) GENERATION.remove(); else GENERATION.set(previous);
    }
    /** The state/block arguments are the exact already-read LVT slots4/5 at the original WARN. */
    public static void unsupported(Species species, BlockPos root, int fertility, BlockState state, Block soil) {
        if (!ENABLED) return;
        observe(() -> {
        List<RootySoilLedger.Caller> callers = StackWalker.getInstance().walk(frames -> frames
                .limit(RootySoilLedger.MAX_CALLERS + 4)
                .filter(frame -> !frame.getClassName().startsWith("com.bettercontent.bettercompatfixes.nativefixture.")
                        && !frame.getClassName().startsWith("com.bettercontent.bettercompatfixes.nativewitness."))
                .limit(RootySoilLedger.MAX_CALLERS)
                .map(frame -> new RootySoilLedger.Caller(frame.getClassName(), frame.getMethodName(), frame.getLineNumber()))
                .toList());
        String name = Thread.currentThread().getName();
        String thread = name.startsWith("Worker-Main-") ? "worldgen" : name.equals("Server thread") ? "server" : "other";
        LEDGER.unsupported(id(species.getRegistryName()), id(ForgeRegistries.BLOCKS.getKey(soil)), pos(root),
                fertility, Block.getId(state), thread, callers);
        return null;
        }, null);
    }
    public static RootySoilLedger.Call setStart(BlockPos pos, BlockState state, int flags) {
        if (!ENABLED) return null;
        return observe(() -> LEDGER.awaitingSet() ? LEDGER.setStart(new RootySoilLedger.SetArgs(pos(pos),
                id(ForgeRegistries.BLOCKS.getKey(state.getBlock())), Block.getId(state), flags)) : null, null);
    }
    public static void setReturn(RootySoilLedger.Call call, boolean result) {
        if (ENABLED) observe(() -> { LEDGER.setReturn(call, result); return null; }, null);
    }
    public static void setThrown(RootySoilLedger.Call call, String exceptionClass) {
        if (ENABLED) observe(() -> { LEDGER.setThrown(call, exceptionClass); return null; }, null);
    }
    private static RootySoilLedger.Pos pos(BlockPos pos) {
        return pos == null ? null : new RootySoilLedger.Pos(pos.getX(), pos.getY(), pos.getZ());
    }
    private static String id(ResourceLocation id) { return id == null ? null : id.toString(); }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) throws IOException {
        if (!ENABLED) return;
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (!cwd.equals(cwd.toRealPath()) || cwd.getParent() == null
                || !cwd.getParent().getFileName().toString().equals("gametest")
                || cwd.getParent().getParent() == null
                || !cwd.getParent().getParent().getFileName().toString().equals("build")
                || !UUID.fromString(cwd.getFileName().toString()).toString().equals(cwd.getFileName().toString()))
            throw new IOException("Rooty witness refuses non-UUID/non-native output directory");
        Files.writeString(cwd.resolve("rooty-soil-witness-v1.json"), LEDGER.sealAndRender(),
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        // No branch logging or mid-run I/O. After-JVM complete DEBUG/latest reconciliation is mandatory.
    }
}
