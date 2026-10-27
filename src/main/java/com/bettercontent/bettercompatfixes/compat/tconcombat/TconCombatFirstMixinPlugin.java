package com.bettercontent.bettercompatfixes.compat.tconcombat;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Separate from the bytecode-pinned legacy compatibility plugin. No optional class linkage. */
public final class TconCombatFirstMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var mods = FMLLoader.getLoadingModList();
        if (mods == null || mods.getModFileById("tconstruct") == null
                || mods.getModFileById("epicfight") == null) return false;
        return mods.getModFileById("epicfight").getMods().stream().anyMatch(mod ->
                "epicfight".equals(mod.getModId()) && "20.14.17".equals(mod.getVersion().toString()));
    }
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
