package com.bettercontent.bettercompatfixes.mixin.kubejs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.latvian.mods.kubejs.util.ConsoleJS", remap = false)
public abstract class ConsoleJSMixin {
    private static final String INITIAL_WORLD_LOG_NOTICE =
            "Due to the way Minecraft resource loading works, KubeJS' server.log may not contain everything "
                    + "that happened in your server scripts on initial world creation.";
    private static final String LATEST_LOG_NOTICE =
            "You can still see the full log (including past reloads) in your latest.log file.";

    @Inject(method = "warn(Ljava/lang/Object;)Ldev/latvian/mods/kubejs/script/ConsoleLine;",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void better_compat_fixes$ignoreInitialWorldLogNotices(
            final Object message,
            final CallbackInfoReturnable<Object> cir
    ) {
        if (INITIAL_WORLD_LOG_NOTICE.equals(message) || LATEST_LOG_NOTICE.equals(message)) {
            cir.setReturnValue(null);
        }
    }
}
