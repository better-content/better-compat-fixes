package com.bettercontent.bettercompatfixes.compat.epicfight;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Independent optional gate; historical provider plugin bytecode remains frozen. */
public final class OptionalTinkerSkinsMixinPlugin implements IMixinConfigPlugin {
    public static boolean selects(String target, String mixin, String epicFight, String tinkerCompat) {
        return "yesman.epicfight.api.client.model.ItemSkinsReloadListener".equals(target)
                && "com.bettercontent.bettercompatfixes.mixin.epicfight.OptionalTinkerItemSkinsMixin".equals(mixin)
                && "20.14.17".equals(epicFight) && "20.2.8".equals(tinkerCompat);
    }

    @Override public boolean shouldApplyMixin(String target, String mixin) {
        var mods = FMLLoader.getLoadingModList();
        return selects(target, mixin, version(mods, "epicfight"), version(mods, "epicfighttinkercompat"));
    }

    private static String version(LoadingModList mods, String id) {
        if (mods == null) return null;
        var file = mods.getModFileById(id);
        if (file == null) return null;
        return file.getMods().stream().filter(mod -> id.equals(mod.getModId()))
                .map(mod -> mod.getVersion().toString()).findFirst().orElse(null);
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
