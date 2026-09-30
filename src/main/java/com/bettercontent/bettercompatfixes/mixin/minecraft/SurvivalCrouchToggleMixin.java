package com.bettercontent.bettercompatfixes.mixin.minecraft;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps the authored vanilla crouch toggle active only in Survival. */
@Mixin(ToggleKeyMapping.class)
public abstract class SurvivalCrouchToggleMixin {
    @Redirect(method = {"setDown", "m_7249_", "isDown", "m_90857_"},
            at = @At(value = "INVOKE",
                    target = "Ljava/util/function/BooleanSupplier;getAsBoolean()Z"),
            require = 2,
            remap = false)
    private boolean betterContentFixes$survivalOnlyCrouchToggle(final BooleanSupplier setting) {
        Minecraft minecraft = Minecraft.getInstance();
        if ((Object) this != minecraft.options.keyShift) return setting.getAsBoolean();
        return setting.getAsBoolean() && minecraft.player != null && minecraft.gameMode != null
                && minecraft.gameMode.getPlayerMode() == GameType.SURVIVAL;
    }
}
