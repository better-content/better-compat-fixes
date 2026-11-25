package com.bettercontent.bettercompatfixes.mixin.emi;

import dev.emi.emi.jemi.JemiUtil;
import dev.emi.emi.jemi.impl.JemiIngredientConsumer;
import dev.emi.emi.jemi.impl.JemiRecipeSlotDrawable;
import dev.emi.emi.jemi.widget.JemiSlotWidget;
import mezz.jei.api.gui.builder.IIngredientConsumer;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Mixin(value = JemiRecipeSlotDrawable.class, remap = false)
public abstract class JemiRecipeSlotDrawableViewMixin implements IRecipeSlotView {
    @Shadow(remap = false) public JemiSlotWidget widget;
    @Shadow(remap = false) public List<IIngredientConsumer> overrides;

    @Override
    public List<ITypedIngredient<?>> getAllIngredientsList() {
        return widget == null ? List.of() : ((IRecipeSlotView) widget.slot).getAllIngredientsList();
    }

    @Override
    public Stream<ITypedIngredient<?>> getDisplayedIngredients() {
        // Display overrides must never become inputs for recipe transfer.
        if (!overrides.isEmpty()) {
            return overrides.stream()
                    .filter(JemiIngredientConsumer.class::isInstance)
                    .map(JemiIngredientConsumer.class::cast)
                    .flatMap(consumer -> consumer.stacks.stream())
                    .flatMap(ingredient -> ingredient.getEmiStacks().stream())
                    .map(JemiUtil::getTyped)
                    .flatMap(Optional::stream);
        }
        return widget == null ? Stream.empty() : ((IRecipeSlotView) widget.slot).getDisplayedIngredients();
    }

    @Override
    public Optional<TagKey<?>> getTagKey() {
        return widget == null ? Optional.empty() : ((IRecipeSlotView) widget.slot).getTagKey();
    }
}
