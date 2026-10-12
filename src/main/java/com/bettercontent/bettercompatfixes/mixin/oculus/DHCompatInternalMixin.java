package com.bettercontent.bettercompatfixes.mixin.oculus;

import com.bettercontent.bettercompatfixes.compat.DhOpaqueDepthPolicy;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiTransparency;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Oculus 1.8.0 copies opaque DH depth only at the transparent-pass event, which DH 2.4.5-b
 * skips for DISABLED transparency. In that mode the connected live depth is already opaque-only.
 * Keep the native copy for FAKE/COMPLETE and read the live setting on every request.
 */
@Mixin(targets = "net.irisshaders.iris.compat.dh.DHCompatInternal", remap = false)
public abstract class DHCompatInternalMixin {
    @Shadow(remap = false)
    private int storedDepthTex;

    @ModifyReturnValue(method = "getDepthTexNoTranslucent()I", at = @At("RETURN"),
            remap = false, require = 2, allow = 2)
    private int better_compat_fixes$liveOpaqueDepthWhenTransparencyDisabled(int copiedDepth) {
        boolean disabled = DhApi.Delayed.configs != null
                && DhApi.Delayed.configs.graphics().transparency().getValue() == EDhApiTransparency.DISABLED;
        return DhOpaqueDepthPolicy.selectDepth(disabled, storedDepthTex, copiedDepth);
    }
}
