package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.nativefixture.IOWorkerMailboxAccessor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual native mailbox/region-file ownership contract; no world worker is paused or replaced. */
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class IOWorkerPendingReadGameTests {
    // UNKNOWN latch/future safety limit, not an empirical I/O performance verdict.
    private static final long HANDSHAKE_SAFETY_SECONDS = 30;

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(IOWorkerPendingReadGameTests.class);
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty",
            batch = "chunk_io_ownership", timeoutTicks = 400)
    public static void pendingChunkReadsOwnTheirNbtBeforeBackgroundFlush(GameTestHelper h) throws Exception {
        Path directory = Files.createDirectories(Path.of("native-evidence", "chunk-io-ownership", UUID.randomUUID().toString()));
        ChunkPos position = new ChunkPos(0, 0);
        CompoundTag stored = new CompoundTag();
        stored.putString("payload", "unchanged");
        CompoundTag nested = new CompoundTag();
        nested.putInt("value", 7);
        stored.put("nested", nested);
        stored.putIntArray("array", new int[] {1, 2, 3});
        CompoundTag expected = stored.copy();
        try (OwnedWorker worker = new OwnedWorker(directory)) {
            inspectPendingRead(worker, position, stored, read -> {
                h.assertTrue(read.isPresent(), "Pending save must remain readable before flush");
                CompoundTag loaded = read.orElseThrow();
                h.assertTrue(loaded != stored, "Pending read exposed the in-flight write root");
                h.assertTrue(expected.equals(loaded), "Pending snapshot changed original payload");
                // The native ChunkStorage upgrade path inserts/removes this ROOT key
                // on a background executor, even when the data version is current.
                loaded.put("__context", new CompoundTag());
                loaded.getCompound("nested").putInt("value", 99);
                loaded.getIntArray("array")[0] = 99;
                h.assertTrue(expected.equals(stored), "Reader mutated the write-owned payload");
                loaded.remove("__context");
            });
            h.assertTrue(expected.equals(await(worker.loadAsync(position)).orElseThrow()),
                    "Persisted region payload was changed by the pending reader");
            inspectPendingRead(worker, position, null, read ->
                    h.assertTrue(read.isEmpty(), "Queued deletion must remain Optional.empty, not an empty chunk"));
            h.assertTrue(await(worker.loadAsync(position)).isEmpty(), "Deletion was not persisted");
            h.assertTrue(await(worker.loadAsync(new ChunkPos(1, 0))).isEmpty(), "Missing disk chunk changed semantics");
        }
        h.succeed();
    }

    private static void inspectPendingRead(OwnedWorker worker, ChunkPos position, CompoundTag payload,
            Consumer<Optional<CompoundTag>> inspect) throws Exception {
        try (Barrier beforeAdmission = new Barrier(worker)) {
            await(beforeAdmission.entered);
            CompletableFuture<Void> write = worker.store(position, payload);
            CompletableFuture<Optional<CompoundTag>> read = worker.loadAsync(position);
            // All three tasks are foreground. The native pending-flush task is
            // background, so this second barrier must run before serialization.
            try (Barrier beforeFlush = new Barrier(worker)) {
                beforeAdmission.close();
                await(beforeFlush.entered);
                if (!read.isDone()) throw new AssertionError("Native foreground read/barrier ordering changed");
                inspect.accept(await(read));
            }
            await(write);
        }
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(HANDSHAKE_SAFETY_SECONDS, TimeUnit.SECONDS);
    }

    private static final class OwnedWorker extends IOWorker {
        OwnedWorker(Path directory) { super(directory, false, "bc-chunk-ownership-fixture"); }
    }

    private static final class Barrier implements AutoCloseable {
        final CompletableFuture<Void> entered = new CompletableFuture<>();
        final CompletableFuture<Void> released = new CompletableFuture<>();
        Barrier(IOWorker worker) {
            ((IOWorkerMailboxAccessor) worker).better_compat_fixes$mailbox().tell(
                    new StrictQueue.IntRunnable(0, () -> {
                        entered.complete(null);
                        released.join();
                    }));
        }
        @Override public void close() { released.complete(null); }
    }
}
