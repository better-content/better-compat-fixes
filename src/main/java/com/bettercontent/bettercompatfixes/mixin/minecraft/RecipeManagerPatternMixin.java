package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.tconstruct.PatternRecipeSubstitution;
import com.google.gson.JsonElement;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = RecipeManager.class, remap = false)
public abstract class RecipeManagerPatternMixin {
    @ModifyVariable(method = {
            "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;"
                    + "Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            "m_5787_(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;"
                    + "Lnet/minecraft/util/profiling/ProfilerFiller;)V"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false, require = 1)
    private Map<ResourceLocation, JsonElement> betterContentFixes$useCanvasForPatterns(
            final Map<ResourceLocation, JsonElement> recipes) {
        if (!ModList.get().isLoaded("tconstruct") || !ModList.get().isLoaded("farmersdelight")) return recipes;
        return PatternRecipeSubstitution.replace(recipes);
    }
}
