package com.bettercontent.bettercompatfixes.mixin.tconstruct;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;

/**
 * TConstruct's KNOWN_VARIANTS is a HashMultimap. Diet expands recipe ingredients while
 * server-start recipe registration can add variants on another thread. A sized HashSet stream
 * being sorted during a put/clear may throw in SortedOps (and can omit food-group inputs).
 * Protect the entire cached read and both mutation paths with the same monitor; do not skip
 * Diet's group inference or alter TConstruct's variant set.
 */
@Mixin(targets = "slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache", remap = false)
public abstract class MaterialRecipeCacheDietMixin {
    @WrapMethod(method = "getAllVariants", remap = false, require = 1)
    private static List<MaterialVariantId> betterContent$readVariants(Operation<List<MaterialVariantId>> original) {
        synchronized (MaterialRecipeCache.class) {
            return original.call();
        }
    }

    @WrapMethod(method = "addKnownVariant", remap = false, require = 1)
    private static void betterContent$addVariant(MaterialVariantId variant, Operation<Void> original) {
        synchronized (MaterialRecipeCache.class) {
            original.call(variant);
        }
    }

    // RecipeCacheInvalidator invokes this callback to clear KNOWN_VARIANTS and SORTED_VARIANTS.
    @WrapMethod(method = "lambda$static$0", remap = false, require = 1)
    private static void betterContent$clearVariants(Operation<Void> original) {
        synchronized (MaterialRecipeCache.class) {
            original.call();
        }
    }
}
