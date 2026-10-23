package com.bettercontent.bettercompatfixes.mixin.complicatedbees;

import com.accbdd.complicated_bees.bees.Chromosome;
import com.accbdd.complicated_bees.bees.GeneticHelper;
import com.bettercontent.bettercompatfixes.compat.bees.DecodedColorScope;
import com.bettercontent.bettercompatfixes.compat.bees.NativeBeeColors;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No extra getChromosome/getSpecies/decode: observe the existing native return inside the exact call scope. */
@Mixin(value = GeneticHelper.class, remap = false)
public abstract class DecodedChromosomeColorMixin {
    @Inject(method = "getChromosome(Lnet/minecraft/world/item/ItemStack;Z)Lcom/accbdd/complicated_bees/bees/Chromosome;",
            at = @At("RETURN"), require = 1, allow = 1)
    private static void betterCompat$alreadyDecodedColor(ItemStack stack, boolean expressed,
                                                        CallbackInfoReturnable<Chromosome> callback) {
        if (!expressed || !DecodedColorScope.capturing()) return;
        DecodedColorScope.Sample sample = NativeBeeColors.fromDecoded(callback.getReturnValue());
        DecodedColorScope.capture(sample.known(), sample.rgb());
    }
}
