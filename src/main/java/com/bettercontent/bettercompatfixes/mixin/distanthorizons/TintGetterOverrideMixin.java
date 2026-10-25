package com.bettercontent.bettercompatfixes.mixin.distanthorizons;

import com.bettercontent.bettercompatfixes.compat.DhTintParentContext;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "loaderCommon.forge.com.seibel.distanthorizons.common.wrappers.block.TintGetterOverride", remap = false)
public abstract class TintGetterOverrideMixin implements DhTintParentContext {
    @Shadow private LevelReader parent;

    @Override
    public void bettercompatfixes$setTintParent(Object wrappedLevel) {
        // A bad wrapper is a contract failure, not a reason to silently disable tint.
        parent = java.util.Objects.requireNonNull((LevelReader) wrappedLevel, "DH tint context level");
    }
}
