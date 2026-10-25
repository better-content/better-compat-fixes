package com.bettercontent.bettercompatfixes.compat.bumblezone;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.core.io.WritingMode;
import java.io.Writer;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

/** Serializes with the original writer, then publishes a complete inode to both original watchers. */
public final class AtomicConfigWriter implements ConfigWriter {
    private final ConfigWriter delegate;
    private final Path target;

    public AtomicConfigWriter(ConfigWriter delegate, Path target) {
        this.delegate = Objects.requireNonNull(delegate);
        this.target = ordinary(target);
    }

    private static Path ordinary(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        for (Path part = absolute; part != null; part = part.getParent())
            if (Files.isSymbolicLink(part)) throw new IllegalArgumentException("Symlink config write path: " + part);
        if (!Files.isRegularFile(absolute, LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("Atomic config target must already be an ordinary file: " + absolute);
        return absolute;
    }

    @Override public void write(UnmodifiableConfig config, Writer writer) {
        delegate.write(config, writer);
    }

    @Override public void write(UnmodifiableConfig config, Path path, WritingMode mode, Charset charset) {
        if (mode != WritingMode.REPLACE || !ordinary(path).equals(target))
            throw new IllegalArgumentException("Unreviewed atomic config write mode/path");
        Path temporary = null;
        try {
            temporary = Files.createTempFile(target.getParent(), ".bc-atomic-config-", ".tmp");
            if (Files.getFileAttributeView(target, PosixFileAttributeView.class) != null)
                Files.setPosixFilePermissions(temporary, Files.getPosixFilePermissions(target));
            delegate.write(config, temporary, WritingMode.REPLACE, charset);
            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
            ordinary(target);
            // Deliberately no truncating fallback: unsupported/failed atomic replacement fails closed.
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            // NightConfig 3.6.4 watches ENTRY_MODIFY, not atomic rename/ENTRY_CREATE.
            // Publish an actual metadata modification AFTER the complete inode is visible;
            // both original callbacks still run and can only read complete TOML.
            Files.setLastModifiedTime(target, FileTime.fromMillis(Files.getLastModifiedTime(target).toMillis() + 1));
        } catch (IOException | RuntimeException failure) {
            // Pre-publication failures retain the original and temp; notification failures
            // also fail closed, with a complete published file (never a truncating fallback).
            throw new WritingException("Atomic Bumblezone config publication failed; retained temp=" + temporary, failure);
        }
    }
}
