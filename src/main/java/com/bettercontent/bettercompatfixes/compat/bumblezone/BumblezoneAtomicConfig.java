package com.bettercontent.bettercompatfixes.compat.bumblezone;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FormatDetector;
import com.electronwill.nightconfig.core.io.ConfigParser;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

/** Public-API-only pinned config registration: original Forge reader/watcher, atomic writer factory. */
public final class BumblezoneAtomicConfig {
    private static final Set<String> FILES = Set.of("the_bumblezone/general.toml", "the_bumblezone/client.toml",
        "the_bumblezone/worldgen.toml", "the_bumblezone/dimension.toml", "the_bumblezone/bee_aggression.toml",
        "the_bumblezone/mod_compatibility.toml");
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();
    private static boolean formatInstalled;
    private BumblezoneAtomicConfig() {}

    public static boolean applies(String version, String file) {
        return "7.13.4+1.20.1-forge".equals(version) && file != null && FILES.contains(file);
    }

    public static void register(ModLoadingContext context, ModConfig.Type type, IConfigSpec<?> spec, String file) {
        var container = context.getActiveContainer();
        if (!"the_bumblezone".equals(container.getModId()) || !applies(container.getModInfo().getVersion().toString(), file))
            throw new IllegalStateException("Unreviewed Bumblezone config registration");
        // Same public registration steps as ModLoadingContext.registerConfig; only the
        // ModConfig's public getHandler extension point differs. Its original reader runs unchanged.
        container.addConfig(new AtomicModConfig(type, spec, container, file));
    }

    private static synchronized void installFormatSupplier() {
        if (formatInstalled) return;
        if (FormatDetector.detectByName("bc-config-check.toml") != TomlFormat.instance())
            throw new IllegalStateException("Unreviewed TOML format registration");
        FormatDetector.registerExtension("toml", (Supplier<ConfigFormat<?>>) () -> {
            Scope scope = ACTIVE.get();
            // Other mods/threads receive the IDENTICAL original singleton, not a wrapper.
            return scope == null ? TomlFormat.instance() : new AtomicTomlFormat(scope);
        });
        formatInstalled = true;
    }

    static BuiltConfig buildWithAtomicWriter(Path path, Supplier<CommentedFileConfig> originalReader) {
        installFormatSupplier();
        Scope previous = ACTIVE.get();
        if (previous != null) throw new IllegalStateException("Nested atomic config construction is unreviewed");
        Scope scope = new Scope(path.toAbsolutePath().normalize(), new AtomicReference<>());
        ACTIVE.set(scope);
        try {
            CommentedFileConfig config = originalReader.get();
            if (scope.writer().get() == null || !config.getNioPath().toAbsolutePath().normalize().equals(scope.path()))
                throw new IllegalStateException("Original Forge reader did not use the scoped atomic writer");
            return new BuiltConfig(config, scope.writer().get());
        } finally { ACTIVE.remove(); }
    }

    static record BuiltConfig(CommentedFileConfig file, AtomicConfigWriter writer) {}
    private static record Scope(Path path, AtomicReference<AtomicConfigWriter> writer) {}

    private static final class AtomicTomlFormat implements ConfigFormat<CommentedConfig> {
        private final Scope scope;
        AtomicTomlFormat(Scope scope) { this.scope = scope; }
        @Override public ConfigWriter createWriter() {
            var writer = new AtomicConfigWriter(TomlFormat.instance().createWriter(), scope.path());
            if (!scope.writer().compareAndSet(null, writer))
                throw new IllegalStateException("Unexpected duplicate scoped TOML writer construction");
            return writer;
        }
        @Override public ConfigParser<CommentedConfig> createParser() { return TomlFormat.instance().createParser(); }
        @Override public CommentedConfig createConfig(Supplier<Map<String, Object>> maps) { return TomlFormat.instance().createConfig(maps); }
        @Override public boolean supportsComments() { return TomlFormat.instance().supportsComments(); }
        @Override public boolean supportsType(Class<?> type) { return TomlFormat.instance().supportsType(type); }
    }

    private static final class AtomicModConfig extends ModConfig {
        private final ConfigFileTypeHandler handler = new AtomicHandler();
        private volatile AtomicConfigWriter writer;
        AtomicModConfig(Type type, IConfigSpec<?> spec, net.minecraftforge.fml.ModContainer container, String file) {
            super(type, spec, container, file);
        }
        @Override public ConfigFileTypeHandler getHandler() { return handler; }
        private final class AtomicHandler extends ConfigFileTypeHandler {
            @Override public Function<ModConfig, CommentedFileConfig> reader(Path base) {
                Function<ModConfig, CommentedFileConfig> original = super.reader(base);
                return config -> {
                    if (config != AtomicModConfig.this) throw new IllegalStateException("Atomic config owner changed");
                    var built = buildWithAtomicWriter(base.resolve(config.getFileName()), () -> original.apply(config));
                    writer = built.writer();
                    return built.file();
                };
            }
        }
    }

    public static boolean isInstalled(ModConfig config) {
        return config instanceof AtomicModConfig atomic && atomic.writer != null && config.getConfigData() instanceof CommentedFileConfig;
    }
}
