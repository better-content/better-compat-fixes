package com.bettercontent.bettercompatfixes.mixin.patchouli;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.common.book.BookRegistry;
import vazkii.patchouli.xplat.XplatModContainer;

import java.io.InputStream;

/**
 * Drops the obsolete Dynamic Trees Ars Nouveau guide registration. Its only
 * book definition relies on Patchouli extension and data-pack modes removed
 * in Patchouli 1.20, so it cannot provide usable pages on this game version.
 */
@Mixin(BookRegistry.class)
public abstract class BookRegistryMixin {
    private static final ResourceLocation OBSOLETE_DT_ARS_GUIDE =
            new ResourceLocation("dtarsnouveau", "guide");

    @Inject(method = "loadBook", at = @At("HEAD"), cancellable = true)
    private void betterContentFixes$skipObsoleteDtArsGuide(
            final XplatModContainer owner,
            final ResourceLocation id,
            final InputStream stream,
            final boolean external,
            final CallbackInfo callback
    ) {
        if (OBSOLETE_DT_ARS_GUIDE.equals(id)) {
            callback.cancel();
        }
    }
}
