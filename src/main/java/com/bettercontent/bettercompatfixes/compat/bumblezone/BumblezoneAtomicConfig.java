package com.bettercontent.bettercompatfixes.compat.bumblezone;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import com.electronwill.nightconfig.core.io.WritingMode;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;

/** Pinned Bz 7.13.4 registers a second raw autoreloader before Forge loads the same files.
 * Forge's later REPLACE save can truncate underneath that independent reader. Keep BOTH
 * original watchers and the complete Forge load/save/reload lifecycle; publish its writes atomically.
 */
public final class BumblezoneAtomicConfig {
    private static final Set<String> FILES = Set.of("the_bumblezone/general.toml", "the_bumblezone/client.toml",
        "the_bumblezone/worldgen.toml", "the_bumblezone/dimension.toml", "the_bumblezone/bee_aggression.toml",
        "the_bumblezone/mod_compatibility.toml");
    private static final String PREFIX = "com.electronwill.nightconfig.core.file.";
    private static final Map<String, String> CLASSES = Map.of(
        PREFIX + "AutosaveCommentedFileConfig", "41b2af1cfed9613a91faacbce9270b29bd67894aa171a7e97d49891dc29c4b5d",
        PREFIX + "SimpleCommentedFileConfig", "011741ee22c06b1c43ef10fada0b55a58f804db465d7b14864f9643d7d8fa8c2",
        PREFIX + "WriteSyncFileConfig", "4e164b690017601606cf2e472a96e1952c8b2f186533a1948963019393ca8da9");
    private BumblezoneAtomicConfig() {}

    public static boolean applies(String version, String file) {
        return "7.13.4+1.20.1-forge".equals(version) && file != null && FILES.contains(file);
    }

    public static void register() {
        ModList.get().getModContainerById("the_bumblezone").ifPresent(container -> {
            if (!applies(container.getModInfo().getVersion().toString(), "the_bumblezone/general.toml")) return;
            if (!(container instanceof FMLModContainer javaMod))
                throw new IllegalStateException("Reviewed Bumblezone Java-FML container changed");
            javaMod.getEventBus().addListener(EventPriority.HIGHEST, false, ModConfigEvent.Loading.class, event -> {
                var config = event.getConfig();
                if ("the_bumblezone".equals(config.getModId()) &&
                    applies(container.getModInfo().getVersion().toString(), config.getFileName())) {
                    if (!(config.getConfigData() instanceof CommentedFileConfig file))
                        throw new IllegalStateException("Reviewed Bumblezone file config changed");
                    install(file);
                }
            });
        });
    }

    private static void reviewed(Class<?> type, String expected) throws Exception {
        try (var stream = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
            if (expected == null || stream == null || !expected.equals(HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes()))))
                throw new IllegalStateException("Unreviewed NightConfig class: " + type.getName());
        }
    }

    private static Field field(Class<?> type, String name, Class<?> expected) throws Exception {
        Field field = type.getDeclaredField(name);
        if (field.getType() != expected) throw new IllegalStateException("NightConfig field ABI changed: " + name);
        field.setAccessible(true);
        return field;
    }

    private static Object synchronousWriter(CommentedFileConfig config) throws Exception {
        Object current = config;
        for (int depth = 0; depth < 3; depth++) {
            Class<?> type = current.getClass();
            reviewed(type, CLASSES.get(type.getName()));
            if (type.getName().equals(PREFIX + "WriteSyncFileConfig")) return current;
            current = field(type, "fileConfig", FileConfig.class).get(current);
            if (!(current instanceof FileConfig file) || !file.getNioPath().equals(config.getNioPath()))
                throw new IllegalStateException("NightConfig wrapper path changed");
        }
        throw new IllegalStateException("Unreviewed NightConfig wrapper depth");
    }

    public static void install(CommentedFileConfig config) {
        try {
            Object sync = synchronousWriter(config);
            synchronized (sync) {
                Field writer = field(sync.getClass(), "writer", ConfigWriter.class);
                Object old = writer.get(sync);
                if (old instanceof AtomicConfigWriter) return;
                if (!(old instanceof ConfigWriter original) ||
                    !old.getClass().getName().equals("com.electronwill.nightconfig.toml.TomlWriter"))
                    throw new IllegalStateException("Unreviewed Bumblezone TOML writer");
                reviewed(old.getClass(), "d2e4bc8ee012d4d96785c3a1918403fae0f64ef57eaa1c19ea120da39ead618f");
                if (field(sync.getClass(), "writingMode", WritingMode.class).get(sync) != WritingMode.REPLACE)
                    throw new IllegalStateException("Unreviewed Bumblezone writing mode");
                writer.set(sync, new AtomicConfigWriter(original, config.getNioPath()));
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot safely publish pinned Bumblezone config", failure);
        }
    }

    public static boolean isInstalled(CommentedFileConfig config) {
        try {
            Object sync = synchronousWriter(config);
            return field(sync.getClass(), "writer", ConfigWriter.class).get(sync) instanceof AtomicConfigWriter;
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot observe pinned Bumblezone config writer", failure);
        }
    }
}
