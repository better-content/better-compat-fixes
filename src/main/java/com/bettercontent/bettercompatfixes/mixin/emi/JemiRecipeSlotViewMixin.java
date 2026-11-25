package com.bettercontent.bettercompatfixes.mixin.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.TagEmiIngredient;
import dev.emi.emi.jemi.impl.JemiRecipeSlot;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Supplies the JEI 15.74 slot-view methods missing from EMI 1.1.24. */
@Mixin(value = JemiRecipeSlot.class, remap = false)
public abstract class JemiRecipeSlotViewMixin implements IRecipeSlotView {
    @Shadow(remap = false) @Final public EmiIngredient stack;

    @Override
    public List<ITypedIngredient<?>> getAllIngredientsList() {
        return getAllIngredients().toList();
    }

    @Override
    public Stream<ITypedIngredient<?>> getDisplayedIngredients() {
        // A plain JEMI slot has no display override; EMI uses its alternatives for display.
        return getAllIngredients();
    }

    @Override
    public Optional<TagKey<?>> getTagKey() {
        return stack instanceof TagEmiIngredient tag ? Optional.of(tag.key) : Optional.empty();
    }
}
