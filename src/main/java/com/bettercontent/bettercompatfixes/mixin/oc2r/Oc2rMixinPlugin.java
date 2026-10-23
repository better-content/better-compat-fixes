package com.bettercontent.bettercompatfixes.mixin.oc2r;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/** Optional absence is inert; a present unreviewed implementation fails closed. */
public final class Oc2rMixinPlugin implements IMixinConfigPlugin {
    public static final String TARGET_SHA256 = "6f4a8b9c92b79f26695b3f21525e3c6c7649f16bb05903c11c8ce0e6d74bf0b8";
    public void onLoad(String pkg) { }
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        var mods = FMLLoader.getLoadingModList();
        if (mods == null) throw new IllegalStateException("OC2R compatibility requires a resolved loading mod list");
        var file = mods.getModFileById("oc2r");
        if (file == null) return false;
        if (file.getMods().stream().noneMatch(m -> m.getModId().equals("oc2r") && m.getVersion().toString().equals("2.2.12")))
            throw new IllegalStateException("OC2R echo compatibility requires exactly 2.2.12");
        try {
            var path = file.getFile().findResource("li/cil/oc2/common/inet/DefaultSessionLayer.class");
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
            if (!hash.equals(TARGET_SHA256)) throw new IllegalStateException("Unreviewed OC2R DefaultSessionLayer bytes: " + hash);
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot validate OC2R echo target bytes", e);
        }
        return true;
    }
    public void acceptTargets(Set<String> own, Set<String> other) { }
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
