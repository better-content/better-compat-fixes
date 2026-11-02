package com.bettercontent.bettercompatfixes.client.tooltip;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Loading-time metadata only. No optional/client classes are linked by this plugin. */
public final class ToolTooltipMixinPlugin implements IMixinConfigPlugin {
    private static boolean supported(String id, String version) {
        var mods = FMLLoader.getLoadingModList();
        if (mods == null || mods.getModFileById(id) == null) return false;
        return mods.getModFileById(id).getMods().stream().anyMatch(mod -> id.equals(mod.getModId())
                && (version == null || version.equals(mod.getVersion().toString())));
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (!supported("tconstruct", "3.11.2.166")) return false;
        if (mixin.endsWith("SatakoTooltipMixin")) return supported("satako", "7.0.33-1.20.1");
        if (mixin.endsWith("QuarkTooltipMixin")) return supported("quark", "4.0-462") && supported("zeta", null);
        if (mixin.endsWith("TinkerWeaponTooltipAccessor")) return supported("epicfight", "20.14.17") && supported("epicfighttinkercompat", "20.2.8");
        if (mixin.endsWith("EpicFightTooltipMixin") || mixin.endsWith("CapabilityItemTooltipAccessor") || mixin.endsWith("WeaponTooltipAccessor"))
            return supported("epicfight", "20.14.17");
        return true;
    }
    @Override public void onLoad(String packageName) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
