package com.bettercontent.bettercompatfixes.mixin.jsonthings;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VanillaPackResourcesBuilder.class)
public abstract class VanillaPackResourcesBuilderMixin {
    @Redirect(
            method = "m_246520_",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/PackType;values()[Lnet/minecraft/server/packs/PackType;",
                    remap = false
            ),
            require = 1,
            remap = false
    )
    private static PackType[] betterContentFixes$builtInPackTypes() {
        return new PackType[]{PackType.CLIENT_RESOURCES, PackType.SERVER_DATA};
    }
}
