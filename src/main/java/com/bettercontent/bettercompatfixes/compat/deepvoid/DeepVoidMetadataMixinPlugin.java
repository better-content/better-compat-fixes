package com.bettercontent.bettercompatfixes.compat.deepvoid;

import com.bettercontent.bettercompatfixes.compat.plugin.BetterContentMixinPlugin;
import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Retains the pinned provider/version policy without changing its historical plugin bytes. */
public final class DeepVoidMetadataMixinPlugin implements IMixinConfigPlugin {
    private final BetterContentMixinPlugin providerPolicy = new BetterContentMixinPlugin();

    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return providerPolicy.shouldApplyMixin(targetClassName, mixinClassName)
                && DeepVoidMetadataNamespace.applies(mixinClassName, FMLEnvironment.production);
    }
    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
