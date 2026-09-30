package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class EmiDefaultsResourceTest {
    @Test
    void constructorSeedsBeforeOptionalEmiInitialization() throws IOException {
        String entrypoint = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/BetterContentFixes.java"));
        String manifest = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));

        assertTrue(entrypoint.contains("EmiDefaultsBootstrap.seedIfApplicable();"));
        assertTrue(manifest.contains("""
                [[dependencies.${mod_id}]]
                    modId="emi"
                    mandatory=false
                    versionRange="[1.1.24,1.2)"
                    ordering="BEFORE"
                    side="CLIENT"
                """), "EMI must remain an optional client dependency ordered after this mod");
    }

    @Test
    void bootstrapRequiresClientEmiAndTconstruct() throws IOException {
        String bootstrap = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/compat/emi/EmiDefaultsBootstrap.java"));

        assertTrue(bootstrap.contains("FMLEnvironment.dist != Dist.CLIENT"));
        assertTrue(bootstrap.contains("!ModList.get().isLoaded(\"emi\")"));
        assertTrue(bootstrap.contains("!ModList.get().isLoaded(\"tconstruct\")"));
    }
}
