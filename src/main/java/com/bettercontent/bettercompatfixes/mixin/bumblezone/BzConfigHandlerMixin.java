package com.bettercontent.bettercompatfixes.mixin.bumblezone;

import com.bettercontent.bettercompatfixes.compat.bumblezone.BumblezoneAtomicConfig;
import com.bettercontent.bettercompatfixes.compat.bumblezone.BumblezoneManualReload;
import com.electronwill.nightconfig.core.file.GenericBuilder;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.telepathicgrunt.the_bumblezone.configs.forge.BzConfigHandler", remap = false)
public abstract class BzConfigHandlerMixin {
    @Redirect(method = "createAndLoadConfigs", at = @At(value = "INVOKE",
        target = "Lcom/electronwill/nightconfig/core/file/GenericBuilder;autoreload()Lcom/electronwill/nightconfig/core/file/GenericBuilder;",
        remap = false), require = 1, remap = false)
    private static GenericBuilder<?, ?> betterContent$ownManualReload(GenericBuilder<?, ?> builder) {
        return BumblezoneManualReload.ownAutoreload(builder);
    }

    @Redirect(method = "createAndLoadConfigs", at = @At(value = "INVOKE",
        target = "Lcom/electronwill/nightconfig/core/file/GenericBuilder;build()Lcom/electronwill/nightconfig/core/file/FileConfig;",
        remap = false), require = 1, remap = false)
    private static FileConfig betterContent$buildManualReload(GenericBuilder<?, ?> builder) {
        return BumblezoneManualReload.buildOwned(builder);
    }

    @Redirect(method = "createAndLoadConfigs", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/fml/ModLoadingContext;registerConfig(Lnet/minecraftforge/fml/config/ModConfig$Type;Lnet/minecraftforge/fml/config/IConfigSpec;Ljava/lang/String;)V",
        remap = false), require = 1, remap = false)
    private static void betterContent$atomicConfig(ModLoadingContext context, ModConfig.Type type, IConfigSpec<?> spec, String file) {
        BumblezoneAtomicConfig.register(context, type, spec, file);
    }
}
