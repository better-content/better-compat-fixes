package com.bettercontent.bettercompatfixes.mixin.supplementaries;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/** No inferred compatibility from version alone; both published and mapped bytes are reviewed. */
public final class TreasureUrnMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String pkg) { }
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        var mods = FMLLoader.getLoadingModList();
        if (mods == null) throw new IllegalStateException("Treasure urn compatibility requires loading mod identity");
        var file = mods.getModFileById("supplementaries");
        if (file == null) return com.bettercontent.bettercompatfixes.compat.TreasureUrnCompatibility.checkVersion(false, null);
        String version = file.getMods().stream().filter(m -> m.getModId().equals("supplementaries")).findFirst().orElseThrow().getVersion().toString();
        com.bettercontent.bettercompatfixes.compat.TreasureUrnCompatibility.checkVersion(true, version);
        try {
            var hashes = new java.util.ArrayList<String>();
            for (String resource : List.of("blocks/UrnBlock.class", "tiles/UrnBlockTile.class")) {
                var path = file.getFile().findResource("net/mehvahdjukaar/supplementaries/common/block/" + resource);
                hashes.add(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
            }
            com.bettercontent.bettercompatfixes.compat.TreasureUrnCompatibility.checkClasses(hashes.get(0), hashes.get(1));
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot verify Supplementaries urn implementation", e);
        }
        return true;
    }
    public void acceptTargets(Set<String> own, Set<String> other) { }
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
