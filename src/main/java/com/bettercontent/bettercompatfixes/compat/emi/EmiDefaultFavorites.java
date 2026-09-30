package com.bettercontent.bettercompatfixes.compat.emi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Creates the pack's editable EMI defaults without ever replacing player-owned data. */
public final class EmiDefaultFavorites {
    static final String FILE_NAME = "emi.json";
    static final String DEFAULT_JSON = """
            {
              "favorites": [
                {"stack": "item:tconstruct:part_builder"},
                {"stack": "item:tconstruct:tinker_station"}
              ],
              "recipe_defaults": {},
              "hidden_stacks": []
            }
            """;

    private EmiDefaultFavorites() {
    }

    public static Result seed(final Path gameDirectory) {
        final Path target = gameDirectory.resolve(FILE_NAME);
        Path staged = null;
        try {
            if (Files.exists(target)) {
                return Result.preserved();
            }
            staged = Files.createTempFile(gameDirectory, ".better-content-emi-", ".json");
            Files.writeString(staged, DEFAULT_JSON, StandardCharsets.UTF_8);
            Files.createLink(target, staged);
            return Result.created();
        } catch (final FileAlreadyExistsException ignored) {
            return Result.preserved();
        } catch (final IOException | UnsupportedOperationException | SecurityException failure) {
            return Result.failed(failure);
        } finally {
            if (staged != null) {
                try {
                    Files.deleteIfExists(staged);
                } catch (final IOException | SecurityException ignored) {
                    // A failed cleanup must not turn a successfully seeded profile into a startup failure.
                }
            }
        }
    }

    public enum Status {
        CREATED,
        PRESERVED,
        FAILED
    }

    public record Result(Status status, Throwable failure) {
        static Result created() {
            return new Result(Status.CREATED, null);
        }

        static Result preserved() {
            return new Result(Status.PRESERVED, null);
        }

        static Result failed(final Throwable failure) {
            return new Result(Status.FAILED, failure);
        }
    }
}
