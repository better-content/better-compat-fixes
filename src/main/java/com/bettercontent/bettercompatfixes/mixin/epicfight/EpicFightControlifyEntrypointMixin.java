package com.bettercontent.bettercompatfixes.mixin.epicfight;

import java.util.Arrays;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;

@Mixin(targets = "yesman.epicfight.compat.controlify.EpicFightControlifyEntrypoint", remap = false)
public abstract class EpicFightControlifyEntrypointMixin {
    @Redirect(
            method = "registerInputBindings",
            at = @At(
                    value = "INVOKE",
                    target = "Lyesman/epicfight/api/client/input/action/EpicFightInputAction;values()[Lyesman/epicfight/api/client/input/action/EpicFightInputAction;"
            ),
            require = 1
    )
    private static EpicFightInputAction[] betterContentFixes$hideModeSwitch() {
        return Arrays.stream(EpicFightInputAction.values())
                .filter(action -> action != EpicFightInputAction.SWITCH_MODE)
                .toArray(EpicFightInputAction[]::new);
    }
}
