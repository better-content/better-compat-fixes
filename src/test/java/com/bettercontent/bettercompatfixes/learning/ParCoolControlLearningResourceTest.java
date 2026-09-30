package com.bettercontent.bettercompatfixes.learning;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ParCoolControlLearningResourceTest {
    private static final Path SOURCE_ROOT = Path.of(
            "src/main/java/com/bettercontent/bettercompatfixes");

    @Test
    void usesTheAuthoritativeServerStartBoundaryAndPersistentEpisode() throws IOException {
        final String source = Files.readString(SOURCE_ROOT.resolve("learning/ParCoolControlLearning.java"));
        final String episodes = Files.readString(SOURCE_ROOT.resolve("learning/CustomControlEpisodes.java"));
        assertTrue(source.contains("ParCoolActionEvent.Start.Post"));
        assertTrue(source.contains("instanceof ServerPlayer"));
        assertTrue(episodes.contains("Player.PERSISTED_NBT_TAG"));
        assertTrue(episodes.contains("CustomControlEpisodeEvent.Kind.FIRST_ACCEPTED"));
        assertTrue(episodes.contains("CustomControlEpisodeEvent.Kind.DISTINCT_SECOND"));
    }

    @Test
    void publishesProviderOwnedEventsWithoutAThreadsDependency() throws IOException {
        final String episodes = Files.readString(SOURCE_ROOT.resolve("learning/CustomControlEpisodes.java"));
        assertTrue(episodes.contains("MinecraftForge.EVENT_BUS.post"));
        assertTrue(!episodes.contains("ThreadSignals"));
    }
}
