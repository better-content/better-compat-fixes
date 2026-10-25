package com.bettercontent.bettercompatfixes.mixin.iceandfire;

import com.bettercontent.bettercompatfixes.compat.DragonEyesModelPaths;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "com.github.alexthe666.iceandfire.client.model.util.TabulaModelHandlerHelper", remap = false)
public abstract class TabulaModelHandlerHelperMixin {
    // Correct the final lookup, AFTER upstream prepends '/' and appends '.tbl'.
    @ModifyArg(method = "loadTabulaModel(Ljava/lang/String;)Lcom/github/alexthe666/citadel/client/model/container/TabulaModelContainer;",
            at = @At(value = "INVOKE", target = "Ljava/lang/ClassLoader;getResourceAsStream(Ljava/lang/String;)Ljava/io/InputStream;"),
            index = 0, require = 1, allow = 1)
    private static String bettercompatfixes$eyeModelResource(String path) {
        return DragonEyesModelPaths.classLoaderPath(path);
    }
}
