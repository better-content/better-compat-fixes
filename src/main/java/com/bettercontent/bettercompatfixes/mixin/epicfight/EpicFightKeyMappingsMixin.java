package com.bettercontent.bettercompatfixes.mixin.epicfight;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import yesman.epicfight.client.input.EpicFightKeyMappings;

@Mixin(value = EpicFightKeyMappings.class, remap = false)
public abstract class EpicFightKeyMappingsMixin {
    @WrapWithCondition(
            method = "registerKeys",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/client/event/RegisterKeyMappingsEvent;register(Lnet/minecraft/client/KeyMapping;)V"
            ),
            require = 1
    )
    private static boolean betterContentFixes$hideModeSwitch(
            final RegisterKeyMappingsEvent event,
            final KeyMapping keyMapping
    ) {
        return keyMapping != EpicFightKeyMappings.SWITCH_MODE;
    }
}
