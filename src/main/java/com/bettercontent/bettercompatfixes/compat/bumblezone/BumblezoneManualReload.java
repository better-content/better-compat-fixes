package com.bettercontent.bettercompatfixes.compat.bumblezone;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfigBuilder;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.core.file.GenericBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Retains the manual file::load operation on its own watcher; Forge owns the default watcher. */
public final class BumblezoneManualReload {
    private static final ThreadLocal<GenericBuilder<?, ?>> PENDING = new ThreadLocal<>();
    private static final class Owner { static final FileWatcher WATCHER = new FileWatcher(); }
    private BumblezoneManualReload() {}

    public static GenericBuilder<?, ?> ownAutoreload(GenericBuilder<?, ?> builder) {
        if (!(builder instanceof CommentedFileConfigBuilder) || PENDING.get() != null)
            throw new IllegalStateException("Unknown/nested Bumblezone manual config builder");
        PENDING.set(builder);
        // Do not register twice on the shared default: NightConfig addWatch is
        // first-wins, not additive. buildOwned installs the SAME load operation
        // before returning the config to the original upstream load/spec code.
        return builder;
    }

    public static FileConfig buildOwned(GenericBuilder<?, ?> builder) {
        try {
            if (PENDING.get() != builder) throw new IllegalStateException("Manual config builder identity changed");
            FileConfig built = builder.build();
            if (!(built instanceof CommentedFileConfig file))
                throw new IllegalStateException("Manual config is not commented/file-backed");
            Path path = file.getNioPath().toAbsolutePath().normalize();
            for (Path part = path; part != null; part = part.getParent())
                if (Files.isSymbolicLink(part)) throw new IllegalArgumentException("Symlink manual config path");
            if (path.getParent() == null || !path.getParent().getFileName().toString().equals("the_bumblezone") ||
                !BumblezoneAtomicConfig.applies("7.13.4+1.20.1-forge", "the_bumblezone/" + path.getFileName()))
                throw new IllegalStateException("Unreviewed manual config path");
            watch(file);
            return file;
        } finally { PENDING.remove(); }
    }

    static void watch(CommentedFileConfig file) {
        try { Owner.WATCHER.addWatch(file.getNioPath(), file::load); }
        catch (IOException failure) { throw new IllegalStateException("Cannot retain Bumblezone manual autoreload", failure); }
    }
}
