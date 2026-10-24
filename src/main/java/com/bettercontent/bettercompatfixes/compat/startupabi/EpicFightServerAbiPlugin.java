package com.bettercontent.bettercompatfixes.compat.startupabi;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Outside the reserved Mixin package. Required config propagates present-input drift as failure. */
public final class EpicFightServerAbiPlugin implements IMixinConfigPlugin {
    public void onLoad(String mixinPackage) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        // Do not validate resources here: Mixin 0.8.5 can merely log generic preparation
        // exceptions. @Pseudo handles absence; all present-input checks run in preApply,
        // whose failures are wrapped as InvalidMixinException and are fatal for this config.
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (!dedicatedServer()) return;
        exactAnchor(target, mixin);
        EpicFightServerAbi.apply(true, target, mixin, resource(), node);
    }
    private static boolean dedicatedServer() {
        if (FMLEnvironment.dist == null) throw new IllegalStateException("Missing physical distribution for Epic Fight ABI correction");
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }
    private static void exactAnchor(String target, String mixin) {
        if (!EpicFightServerAbi.TARGET.equals(target) || !EpicFightServerAbi.MIXIN.equals(mixin))
            throw new IllegalStateException("Unexpected Epic Fight ABI anchor/target");
    }
    private static byte[] resource() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) throw new IllegalStateException("Missing Epic Fight ABI resource loader");
        try (InputStream input = loader.getResourceAsStream(EpicFightServerAbi.RESOURCE)) {
            if (input == null) return null;
            byte[] bytes = input.readNBytes(2 * 1024 * 1024 + 1);
            if (bytes.length > 2 * 1024 * 1024) throw new IllegalStateException("Oversized Epic Fight ABI resource");
            return bytes;
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot inspect Epic Fight ABI resource", failure);
        }
    }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
