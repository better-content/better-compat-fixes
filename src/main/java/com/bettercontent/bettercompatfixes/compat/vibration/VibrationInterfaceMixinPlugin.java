package com.bettercontent.bettercompatfixes.compat.vibration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Public metadata/resource APIs only. Absence is optional; unknown present inputs fail closed. */
public final class VibrationInterfaceMixinPlugin implements IMixinConfigPlugin {
    static final String CONFIG_SHA = "aebb8ccd7f96d38545dedff06608fae0e8840c7037ad30c2a0623c9fa7440f17";
    static final String RAW_CLASS_SHA = "399ca37359fbbf851eb54f5e3524c59a25751998cae0c9f534bb68a45f56659f";
    static final String NAMED_CLASS_SHA = "76ace04205f06a85fda9077f8240067084dd6febead6c2041c2f967584715613";
    private static final String MIXIN = "com.bettercontent.bettercompatfixes.mixin.vibration.VibrationTickerMixin";
    private boolean permitted;
    static void binding(String version, String config, String originalClass, boolean production) {
        if (!version.equals("2.4.11") || !config.equals(CONFIG_SHA) ||
            !originalClass.equals(production ? RAW_CLASS_SHA : NAMED_CLASS_SHA))
            throw new IllegalStateException("VS vibration replacement requires exact 2.4.11 revision-3 resource transfer and untouched raw/named class");
    }
    private static String digest(Path path) {
        try (var input = Files.newInputStream(path)) {
            byte[] data = input.readNBytes(65537);
            if (data.length > 65536) throw new IllegalStateException("Oversize VS vibration binding resource");
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (IOException | NoSuchAlgorithmException error) {
            throw new IllegalStateException("Unable to verify exact VS vibration binding", error);
        }
    }
    private static void identity(String target, String mixin) {
        if (!target.equals(VibrationInterfaceTransform.TARGET.replace('/', '.')) || !mixin.equals(MIXIN))
            throw new IllegalStateException("Unknown VS vibration interface target/mixin");
    }
    @Override public void onLoad(String mixinPackage) {
        if (!mixinPackage.equals("com.bettercontent.bettercompatfixes.mixin.vibration"))
            throw new IllegalStateException("Unknown VS vibration namespace");
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        identity(target,mixin);
        var loading=FMLLoader.getLoadingModList();
        if(loading==null) throw new IllegalStateException("VS vibration gate lacks loading mod metadata");
        var file=loading.getModFileById("valkyrienskies");
        if(file==null) return false;
        var mod=file.getMods().stream().filter(m->m.getModId().equals("valkyrienskies")).findFirst().orElseThrow();
        binding(mod.getVersion().toString(), digest(file.getFile().findResource("valkyrienskies-common.mixins.json")),
            digest(file.getFile().findResource("org/valkyrienskies/mod/mixin/feature/sculk/MixinVibrationSystemTicker.class")), FMLEnvironment.production);
        permitted=true; return true;
    }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { identity(target,mixin); }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        identity(target,mixin);
        if(!permitted) throw new IllegalStateException("VS vibration transfer not bound to a verified provider");
        VibrationInterfaceTransform.apply(node);
    }
}
