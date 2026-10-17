package com.bettercontent.bettercompatfixes.mixin.epicfight;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.AnimationSubFileReader.PovSettings;

@Mixin(targets = "yesman.epicfight.api.client.animation.AnimationSubFileReader$PovAnimationDeserializer",
        remap = false)
public abstract class EpicFightClimbCameraMixin {
    @Inject(
            method = "deserialize(Lyesman/epicfight/api/animation/types/StaticAnimation;Lcom/google/gson/JsonElement;)Lyesman/epicfight/api/client/animation/AnimationSubFileReader$PovSettings;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            expect = 1,
            allow = 1,
            remap = false
    )
    private void betterContentFixes$leaveClimbCameraFree(
            final StaticAnimation animation,
            final JsonElement json,
            final CallbackInfoReturnable<PovSettings> callback
    ) {
        final ResourceLocation name = animation.getRegistryName();
        if (name == null || !"epicfight".equals(name.getNamespace())
                || !"biped/living/climb".equals(name.getPath())) return;

        final PovSettings original = callback.getReturnValue();
        if (original == null || original.viewLimit() == null) return;

        // Native ladder/vine climbing must not recenter the player's view when it starts.
        // Preserve all animation/camera transforms; remove only its pitch/yaw restriction.
        callback.setReturnValue(new PovSettings(
                original.cameraTransform(),
                original.visibilities(),
                original.rootTransformation(),
                null,
                original.visibilityOthers(),
                original.hasUniqueAnimation(),
                original.syncFrame()
        ));
    }
}
