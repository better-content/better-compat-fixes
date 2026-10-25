package com.bettercontent.bettercompatfixes.mixin.distanthorizons;

import com.bettercontent.bettercompatfixes.compat.DhTintParentContext;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "loaderCommon.forge.com.seibel.distanthorizons.common.wrappers.block.AbstractDhTintGetter", remap = false)
public abstract class AbstractDhTintGetterMixin {
    @Shadow protected IClientLevelWrapper clientLevelWrapper;

    @Inject(method = "update(LloaderCommon/forge/com/seibel/distanthorizons/common/wrappers/block/BiomeWrapper;LloaderCommon/forge/com/seibel/distanthorizons/common/wrappers/block/BlockStateWrapper;Lcom/seibel/distanthorizons/core/dataObjects/fullData/sources/FullDataSourceV2;Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/IClientLevelWrapper;)V",
            at = @At("RETURN"), require = 1, allow = 1)
    private void bettercompatfixes$refreshTintContext(CallbackInfo ci) {
        if ((Object) this instanceof DhTintParentContext context) {
            // getColor invokes this inherited four-argument overload, not the overload
            // that sets parent. Bind EVERY update to its own world, never Minecraft.level
            // or a previous ThreadLocal invocation's dimension. The explicit five-arg
            // overload still overwrites this default after super.update returns.
            context.bettercompatfixes$setTintParent(clientLevelWrapper.getWrappedMcObject());
        }
    }
}
