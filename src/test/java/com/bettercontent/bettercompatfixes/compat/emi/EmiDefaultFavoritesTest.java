package com.bettercontent.bettercompatfixes.compat.emi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class EmiDefaultFavoritesTest {
    @TempDir
    Path gameDirectory;

    @Test
    void missingFileReceivesTheTwoOrderedTconstructFavorites() throws IOException {
        EmiDefaultFavorites.Result result = EmiDefaultFavorites.seed(gameDirectory);

        assertEquals(EmiDefaultFavorites.Status.CREATED, result.status());
        assertEquals(null, result.failure());

        JsonObject root = JsonParser.parseReader(Files.newBufferedReader(
                gameDirectory.resolve("emi.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(3, root.size());
        assertEquals("item:tconstruct:part_builder",
                root.getAsJsonArray("favorites").get(0).getAsJsonObject().get("stack").getAsString());
        assertEquals("item:tconstruct:tinker_station",
                root.getAsJsonArray("favorites").get(1).getAsJsonObject().get("stack").getAsString());
        assertTrue(root.getAsJsonObject("recipe_defaults").isEmpty());
        assertTrue(root.getAsJsonArray("hidden_stacks").isEmpty());
        assertNoStagingFiles();
    }

    @Test
    void existingPlayerDataIsPreservedByteForByteEvenWhenMalformed() throws IOException {
        Path target = gameDirectory.resolve("emi.json");
        byte[] playerData = new byte[] {0x00, 0x13, (byte) 0xff, 0x42};
        Files.write(target, playerData);

        EmiDefaultFavorites.Result result = EmiDefaultFavorites.seed(gameDirectory);

        assertEquals(EmiDefaultFavorites.Status.PRESERVED, result.status());
        assertArrayEquals(playerData, Files.readAllBytes(target));
        assertNoStagingFiles();
    }

    @Test
    void concurrentSeedersCreateOnceWithoutReplacingTheWinner() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<EmiDefaultFavorites.Result> seed = () -> {
            ready.countDown();
            start.await();
            return EmiDefaultFavorites.seed(gameDirectory);
        };

        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(seed);
            var second = executor.submit(seed);
            ready.await();
            start.countDown();

            List<EmiDefaultFavorites.Status> statuses = List.of(first.get().status(), second.get().status());
            assertEquals(1, statuses.stream().filter(status -> status == EmiDefaultFavorites.Status.CREATED).count());
            assertEquals(1, statuses.stream().filter(status -> status == EmiDefaultFavorites.Status.PRESERVED).count());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(EmiDefaultFavorites.DEFAULT_JSON,
                Files.readString(gameDirectory.resolve("emi.json"), StandardCharsets.UTF_8));
        assertNoStagingFiles();
    }

    @Test
    void ioFailureIsReportedWithoutEscapingToStartup() throws IOException {
        Path notADirectory = gameDirectory.resolve("file");
        Files.writeString(notADirectory, "occupied", StandardCharsets.UTF_8);

        EmiDefaultFavorites.Result result = EmiDefaultFavorites.seed(notADirectory);

        assertEquals(EmiDefaultFavorites.Status.FAILED, result.status());
        assertNotNull(result.failure());
        assertFalse(Files.exists(gameDirectory.resolve("file/emi.json")));
    }

    private void assertNoStagingFiles() throws IOException {
        try (var children = Files.list(gameDirectory)) {
            assertTrue(children.noneMatch(path -> path.getFileName().toString().startsWith(".better-content-emi-")));
        }
    }
}
