package com.bettercontent.bettercompatfixes.mixin.distanthorizons;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.seibel.distanthorizons.core.network.exceptions.RequestRejectedException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Semaphore;

@Mixin(targets = "com.seibel.distanthorizons.core.multiplayer.client.AbstractFullDataNetworkRequestQueue", remap = false)
public abstract class AbstractFullDataNetworkRequestQueueMixin {
    @Shadow
    private Semaphore pendingTasksSemaphore;

    @Inject(method = "handleNetResponse", at = @At("HEAD"), cancellable = true, remap = false)
    private void better_compat_fixes$cancelRejectedStaleRequest(
            @Coerce Object task,
            @Coerce Object response,
            Throwable throwable,
            CallbackInfo ci
    ) {
        if (!BcFixesConfig.lostCitiesCancelStaleDhClientRequests()
                || throwable == null
                || !(throwable instanceof RequestRejectedException)
                || !isDimensionSwitchRejection(throwable)) {
            return;
        }

        pendingTasksSemaphore.release();
        ((NetRequestTaskAccessor) task).better_compat_fixes$getFuture().cancel(false);
        ci.cancel();
    }

    private static boolean isDimensionSwitchRejection(Throwable throwable) {
        String message = throwable.getMessage();
        return message != null
                && message.contains("Generation not allowed")
                && message.contains("player dimension")
                && message.contains("handler dimension");
    }

}
