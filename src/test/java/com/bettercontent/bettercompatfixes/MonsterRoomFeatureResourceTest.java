package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class MonsterRoomFeatureResourceTest {
    @Test
    void protectedDungeonOriginReturnsFailureBeforeVanillaLogsSuccessAsAnError() throws IOException {
        String config = Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json"));
        String source = Files.readString(Path.of(
                "src/main/java/com/bettercontent/bettercompatfixes/mixin/minecraft/MonsterRoomFeatureMixin.java"));

        assertTrue(config.contains("minecraft.MonsterRoomFeatureMixin"));
        assertTrue(source.contains("@Mixin(MonsterRoomFeature.class)"));
        assertTrue(source.contains("method = {\"place\", \"m_142674_\"}"));
        assertTrue(source.contains("Lorg/slf4j/Logger;error(Ljava/lang/String;[Ljava/lang/Object;)V"));
        assertTrue(source.contains("cancellable = true"));
        assertTrue(source.contains("require = 1"));
        assertTrue(source.contains("callback.setReturnValue(false)"));
        assertTrue(!source.contains("SpawnerBlockEntity"));
        assertTrue(!source.contains("setBlockEntity"));
    }
}
