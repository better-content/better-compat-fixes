package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.compat.VanillaBoatPolicy;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Boat.class)
public abstract class BoatMixin {
    @ModifyConstant(method = "hurt", constant = @Constant(floatValue = 40.0F))
    private float betterContentFixes$scaleVanillaDestructionThreshold(final float vanillaThreshold) {
        return VanillaBoatPolicy.destructionThreshold((Boat) (Object) this, vanillaThreshold);
    }

    /** Replaces the vessel item with partial materials while leaving vanilla destruction intact. */
    @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
    private void betterContentFixes$dropDestructionComponents(
            final DamageSource source,
            final CallbackInfo callback) {
        final Boat boat = (Boat) (Object) this;
        for (final var component : VanillaBoatPolicy.destructionComponents(boat)) {
            boat.spawnAtLocation(component);
        }
        if (VanillaBoatPolicy.suppressVesselDrop(boat)) {
            callback.cancel();
        }
    }
}
