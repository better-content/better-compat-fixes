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
public abstract class EpicParCoolLedgeCameraMixin {
    @Inject(
            method = "deserialize(Lyesman/epicfight/api/animation/types/StaticAnimation;Lcom/google/gson/JsonElement;)Lyesman/epicfight/api/client/animation/AnimationSubFileReader$PovSettings;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            expect = 1,
            allow = 1,
            remap = false
    )
    private void betterContentFixes$leaveLedgeCameraFree(
            final StaticAnimation animation,
            final JsonElement json,
            final CallbackInfoReturnable<PovSettings> callback
    ) {
        final ResourceLocation name = animation.getRegistryName();
        if (name == null || !"epicparcool".equals(name.getNamespace())) return;
        final boolean ledgeHang = switch (name.getPath()) {
            case "biped/cling_to_cliff",
                    "biped/cling_to_cliff_inner_corner",
                    "biped/cling_to_cliff_outer_corner",
                    "biped/cling_to_cliff_left",
                    "biped/cling_to_cliff_right" -> true;
            default -> false;
        };
        if (!ledgeHang) return;

        final PovSettings original = callback.getReturnValue();
        if (original == null || original.viewLimit() == null) return;

        // A view-limit violation makes Epic Fight recenter the camera on model yaw,
        // even when only pitch violated the limit. Remove only the ledge POV limits
        // at load time so that correction cannot start or carry into pulling up.
        // Keep body/root animation, visibility, and camera transforms unchanged.
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
