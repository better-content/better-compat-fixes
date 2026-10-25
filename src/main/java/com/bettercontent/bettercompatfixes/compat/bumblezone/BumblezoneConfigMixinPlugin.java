package com.bettercontent.bettercompatfixes.compat.bumblezone;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** No game-class loads, reflection or historical-plugin changes during optional target selection. */
public final class BumblezoneConfigMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (!target.equals("com.telepathicgrunt.the_bumblezone.configs.forge.BzConfigHandler") ||
            !mixin.equals("com.bettercontent.bettercompatfixes.mixin.bumblezone.BzConfigHandlerMixin"))
            throw new IllegalStateException("Unknown Bumblezone config mixin target");
        var loading = FMLLoader.getLoadingModList();
        if (loading == null) throw new IllegalStateException("Bumblezone config gate lacks loading mod metadata");
        var file = loading.getModFileById("the_bumblezone");
        return file != null && file.getMods().stream().anyMatch(mod -> mod.getModId().equals("the_bumblezone") &&
            mod.getVersion().toString().equals("7.13.4+1.20.1-forge"));
    }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
