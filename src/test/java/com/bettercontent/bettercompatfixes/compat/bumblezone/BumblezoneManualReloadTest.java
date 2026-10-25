package com.bettercontent.bettercompatfixes.compat.bumblezone;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BumblezoneManualReloadTest {
    @Test void realSharedWatcherIsFirstWinsNotAdditive(@TempDir Path root) throws Exception {
        Path path = root.resolve("baseline.toml"); Files.writeString(path, "value=1\n");
        var first = new CountDownLatch(1); var second = new CountDownLatch(1);
        var watcher = new FileWatcher();
        try {
            watcher.addWatch(path, first::countDown); watcher.addWatch(path, second::countDown);
            publish(path, 2);
            assertTrue(first.await(3, TimeUnit.SECONDS));
            assertFalse(second.await(150, TimeUnit.MILLISECONDS), "Pinned addWatch must expose the original callback collision");
        } finally { watcher.stop(); }
    }
    @Test void ownedOriginalLoadAndSeparateForgeStyleWatcherBothRemainLive(@TempDir Path root) throws Exception {
        Path path = root.resolve("the_bumblezone/general.toml"); Files.createDirectories(path.getParent());
        Files.writeString(path, "value=1\n");
        var builder = CommentedFileConfig.builder(path).preserveInsertionOrder();
        assertSame(builder, BumblezoneManualReload.ownAutoreload(builder)); builder.sync();
        try (var manual = (CommentedFileConfig) BumblezoneManualReload.buildOwned(builder)) {
            manual.load(); assertEquals(1, manual.<Integer>get("value"));
            var watcher = new FileWatcher();
            try {
                for (int value = 2; value <= 4; value++) {
                    var forge = new CountDownLatch(1);
                    watcher.setWatch(path, forge::countDown);
                    publish(path, value);
                    assertTrue(forge.await(3, TimeUnit.SECONDS), "Separate Forge-style watcher lost publication");
                    // Bounded asynchronous observation, not a delay in production or a replacement callback.
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                    while (!Integer.valueOf(value).equals(manual.get("value")) && System.nanoTime() < deadline) Thread.yield();
                    assertEquals(value, manual.<Integer>get("value"), "The actual retained file::load operation did not run");
                }
            } finally { watcher.stop(); }
        }
    }
    @Test void unknownBuilderSequenceAndPathFailClosed(@TempDir Path root) {
        var first = CommentedFileConfig.builder(root.resolve("unknown.toml")).sync();
        var second = CommentedFileConfig.builder(root.resolve("other.toml")).sync();
        BumblezoneManualReload.ownAutoreload(first);
        assertThrows(IllegalStateException.class, () -> BumblezoneManualReload.ownAutoreload(second));
        assertThrows(IllegalStateException.class, () -> BumblezoneManualReload.buildOwned(second));
        BumblezoneManualReload.ownAutoreload(first);
        assertThrows(IllegalStateException.class, () -> BumblezoneManualReload.buildOwned(first));
    }
    private static void publish(Path path, int value) {
        var config = CommentedConfig.inMemory(); config.set("value", value);
        new AtomicConfigWriter(TomlFormat.instance().createWriter(), path).write(config, path, WritingMode.REPLACE, StandardCharsets.UTF_8);
    }
}
