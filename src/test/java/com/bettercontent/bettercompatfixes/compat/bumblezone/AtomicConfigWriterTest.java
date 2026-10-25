package com.bettercontent.bettercompatfixes.compat.bumblezone;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class AtomicConfigWriterTest {
    private static final String OLD = "[\"Variant Bee Options\"]\nvalue = 1\n";
    private static final String NEW = "[\"Variant Bee Options\"]\nvalue = 2\n";
    private static Config read(Path path) throws Exception {
        try (var reader = Files.newBufferedReader(path)) { return TomlFormat.instance().createParser().parse(reader); }
    }
    private static Path seeded(Path root, String name) throws Exception {
        Path path = root.resolve(name); Files.writeString(path, OLD); return path;
    }
    private static ConfigWriter paused(CountDownLatch half, CountDownLatch finish) {
        return (config, writer) -> {
            try {
                writer.write("[\"Variant"); writer.flush(); half.countDown();
                if (!finish.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Test publication latch expired");
                writer.write(" Bee Options\"]\nvalue = 2\n");
            } catch (Exception failure) { throw new WritingException(failure); }
        };
    }
    @Test void originalReplaceDeterministicallyExposesTruncatedToml(@TempDir Path root) throws Exception {
        Path path = seeded(root, "baseline.toml"); var half = new CountDownLatch(1); var finish = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var task = executor.submit(() -> paused(half, finish).write(Config.inMemory(), path, WritingMode.REPLACE));
            assertTrue(half.await(5, TimeUnit.SECONDS));
            assertThrows(ParsingException.class, () -> read(path));
            finish.countDown(); task.get(5, TimeUnit.SECONDS); assertEquals(NEW, Files.readString(path));
        } finally { finish.countDown(); executor.shutdownNow(); }
    }
    @Test void independentReaderSeesCompleteOldThenCompleteNewInode(@TempDir Path root) throws Exception {
        Path path = seeded(root, "atomic.toml"); var half = new CountDownLatch(1); var finish = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var writer = new AtomicConfigWriter(paused(half, finish), path);
            var task = executor.submit(() -> writer.write(Config.inMemory(), path, WritingMode.REPLACE));
            assertTrue(half.await(5, TimeUnit.SECONDS));
            assertEquals(OLD, Files.readString(path)); assertEquals(1, read(path).<Integer>get(List.of("Variant Bee Options", "value")));
            finish.countDown(); task.get(5, TimeUnit.SECONDS);
            assertEquals(NEW, Files.readString(path)); assertEquals(2, read(path).<Integer>get(List.of("Variant Bee Options", "value")));
        } finally { finish.countDown(); executor.shutdownNow(); }
    }
    @Test void actualNightConfigWatcherStillReceivesAndParsesReplacement(@TempDir Path root) throws Exception {
        Path path = seeded(root, "watched.toml"); var first = new CountDownLatch(1); var second = new CountDownLatch(1);
        var failures = new CopyOnWriteArrayList<Exception>(); var watcher = new FileWatcher(failures::add);
        try {
            watcher.addWatch(path, () -> { try {
                int value = read(path).get(List.of("Variant Bee Options", "value"));
                if (value == 2) first.countDown(); if (value == 3) second.countDown();
            } catch (Exception failure) { failures.add(failure); } });
            for (int value : new int[]{2, 3}) {
                String text = NEW.replace("value = 2", "value = " + value);
                new AtomicConfigWriter((config, writer) -> { try { writer.write(text); } catch (Exception e) { throw new WritingException(e); } }, path)
                    .write(Config.inMemory(), path, WritingMode.REPLACE);
                assertTrue((value == 2 ? first : second).await(5, TimeUnit.SECONDS), "Original watcher must survive each inode replacement");
            }
            assertEquals(List.of(), failures);
        } finally { watcher.stop(); }
    }
    @Test void originalTomlWriterRetainsValuesCommentsAndEncoding(@TempDir Path root) throws Exception {
        Path path = seeded(root, "values.toml");
        try (var config = CommentedFileConfig.builder(path).sync().preserveInsertionOrder().build()) {
            config.load(); config.setComment(List.of("Variant Bee Options", "value"), " Authored λ comment");
            config.set(List.of("Variant Bee Options", "value"), 17);
            var original = TomlFormat.instance().createWriter(); String expected = original.writeToString(config);
            new AtomicConfigWriter(original, path).write(config, path, WritingMode.REPLACE, StandardCharsets.UTF_8);
            assertEquals(expected, Files.readString(path)); assertEquals(17, read(path).<Integer>get(List.of("Variant Bee Options", "value")));
        }
    }
    @Test void failedSerializationRetainsOriginalAndFailedTemp(@TempDir Path root) throws Exception {
        Path path = seeded(root, "failure.toml");
        ConfigWriter bad = (config, writer) -> { try { writer.write("[\"Variant"); } catch (Exception e) { throw new WritingException(e); } throw new WritingException("injected"); };
        assertThrows(WritingException.class, () -> new AtomicConfigWriter(bad, path).write(Config.inMemory(), path, WritingMode.REPLACE));
        assertEquals(OLD, Files.readString(path));
        try (var paths = Files.list(root)) { assertEquals(1, paths.filter(p -> p.getFileName().toString().startsWith(".bc-atomic-config-")).count()); }
    }
    @Test void rejectsUnreviewedModePathAndSymlinksBeforeWriting(@TempDir Path root) throws Exception {
        Path path = seeded(root, "bound.toml"), other = seeded(root, "other.toml");
        var writer = new AtomicConfigWriter(TomlFormat.instance().createWriter(), path);
        assertThrows(IllegalArgumentException.class, () -> writer.write(Config.inMemory(), path, WritingMode.APPEND));
        assertThrows(IllegalArgumentException.class, () -> writer.write(Config.inMemory(), other, WritingMode.REPLACE));
        Path link = root.resolve("link.toml"); Files.createSymbolicLink(link, path);
        assertThrows(IllegalArgumentException.class, () -> new AtomicConfigWriter(TomlFormat.instance().createWriter(), link));
        assertEquals(OLD, Files.readString(path)); assertEquals(OLD, Files.readString(other));
    }
    @Test void publicFactoryBuildsActualForgeStyleConfigsWithoutChangingOtherThreads(@TempDir Path root) throws Exception {
        for (boolean autosave : new boolean[]{false, true}) {
            Path path = seeded(root, "wrapper-" + autosave + ".toml");
            var executor = Executors.newSingleThreadExecutor();
            try {
                var built = BumblezoneAtomicConfig.buildWithAtomicWriter(path, () -> {
                    try {
                        assertSame(TomlFormat.instance(), executor.submit(() ->
                            com.electronwill.nightconfig.core.file.FormatDetector.detectByName("other-mod.toml")).get(5, TimeUnit.SECONDS));
                    } catch (Exception e) { throw new IllegalStateException(e); }
                    var builder = CommentedFileConfig.builder(path).sync().preserveInsertionOrder();
                    if (autosave) builder.autosave();
                    var file = (CommentedFileConfig) builder.build(); file.load(); return file;
                });
                try (var file = built.file()) {
                    assertNotNull(built.writer());
                    assertSame(TomlFormat.instance(), com.electronwill.nightconfig.core.file.FormatDetector.detectByName("other-mod.toml"));
                    file.set(List.of("Variant Bee Options", "value"), 23); file.save();
                    assertEquals(23, read(path).<Integer>get(List.of("Variant Bee Options", "value")));
                }
            } finally { executor.shutdownNow(); }
        }
    }
    @Test void failedNestedOrMismatchedConstructionAlwaysClearsPublicFactoryScope(@TempDir Path root) throws Exception {
        Path path = seeded(root, "scope.toml"), other = seeded(root, "other.toml");
        assertThrows(IllegalStateException.class, () -> BumblezoneAtomicConfig.buildWithAtomicWriter(path, () ->
            BumblezoneAtomicConfig.buildWithAtomicWriter(path, () -> CommentedFileConfig.of(path)).file()));
        assertSame(TomlFormat.instance(), com.electronwill.nightconfig.core.file.FormatDetector.detectByName("normal.toml"));
        try (var foreign = CommentedFileConfig.of(other)) {
            assertThrows(IllegalStateException.class, () -> BumblezoneAtomicConfig.buildWithAtomicWriter(path, () -> foreign));
        }
        assertSame(TomlFormat.instance(), com.electronwill.nightconfig.core.file.FormatDetector.detectByName("normal.toml"));
        assertEquals(OLD, Files.readString(path)); assertEquals(OLD, Files.readString(other));
    }
    @Test void optionalVersionAndFileScopeAreExact() {
        assertTrue(BumblezoneAtomicConfig.applies("7.13.4+1.20.1-forge", "the_bumblezone/general.toml"));
        for (String version : new String[]{null, "", "7.13.4", "7.13.5+1.20.1-forge"})
            assertFalse(BumblezoneAtomicConfig.applies(version, "the_bumblezone/general.toml"));
        for (String file : new String[]{null, "", "other/general.toml", "the_bumblezone/../general.toml", "the_bumblezone/unreviewed.toml"})
            assertFalse(BumblezoneAtomicConfig.applies("7.13.4+1.20.1-forge", file));
    }
}
