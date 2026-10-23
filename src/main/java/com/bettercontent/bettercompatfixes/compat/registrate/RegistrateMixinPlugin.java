package com.bettercontent.bettercompatfixes.compat.registrate;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/** Class-resource inspection only: absence inert, unknown/partial present implementation fails closed. */
public final class RegistrateMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String pkg) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) throw new IllegalStateException("Missing Registrate resource loader");
        try {
            return RegistrateCompatibility.check(read(loader, "com/tterrag/registrate/AbstractRegistrate.class"),
                read(loader, "com/tterrag/registrate/AbstractRegistrate$Registration.class"));
        } catch (IOException failure) { throw new IllegalStateException("Cannot inspect present Registrate ABI", failure); }
    }
    private static byte[] read(ClassLoader loader, String name) throws IOException {
        try (InputStream input = loader.getResourceAsStream(name)) {
            if (input == null) return null;
            byte[] bytes = input.readNBytes(2 * 1024 * 1024 + 1);
            if (bytes.length > 2 * 1024 * 1024) throw new IOException("Oversized Registrate class resource");
            return bytes;
        }
    }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
