package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.client.PerspectiveControls;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Both inputs traverse vanilla's renderer updates and Epic Fight's camera-change callbacks. */
@Mixin(Minecraft.class)
public abstract class PerspectiveControlsMixin {
    @Unique private CameraType better_compat_fixes$requestedPerspective;

    @WrapOperation(method = {"handleKeybinds", "m_91279_"}, at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z", ordinal = 0),
            @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;m_90859_()Z", ordinal = 0)
    }, remap = false, require = 1, allow = 1)
    private boolean better_compat_fixes$consumePerspectiveInput(
            final KeyMapping rear, final Operation<Boolean> original) {
        // Consume both queues on each iteration, even if rear wins this iteration.
        final boolean rearClicked = original.call(rear);
        final boolean frontClicked = PerspectiveControls.FRONT.consumeClick();
        better_compat_fixes$requestedPerspective = PerspectiveControls.requestedView(rearClicked, frontClicked);
        return better_compat_fixes$requestedPerspective != null;
    }

    @WrapOperation(method = {"handleKeybinds", "m_91279_"}, at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;cycle()Lnet/minecraft/client/CameraType;"),
            @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;m_90614_()Lnet/minecraft/client/CameraType;")
    }, remap = false, require = 1, allow = 1)
    private CameraType better_compat_fixes$toggleRequestedPerspective(
            final CameraType current, final Operation<CameraType> original) {
        return PerspectiveControls.toggle(current, better_compat_fixes$requestedPerspective);
    }
}
