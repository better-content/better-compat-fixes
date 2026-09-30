package com.bettercontent.bettercompatfixes.mixin.tconstruct;

import com.illusivesoulworks.polymorph.common.crafting.RecipeSelection;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import slimeknights.tconstruct.tables.block.entity.table.CraftingStationBlockEntity;

import java.util.Optional;

@Mixin(CraftingStationBlockEntity.class)
public abstract class CraftingStationPolymorphMixin {
    @Redirect(
            method = "calcResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor("
                            + "Lnet/minecraft/world/item/crafting/RecipeType;"
                            + "Lnet/minecraft/world/Container;"
                            + "Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"))
    private <C extends Container, T extends Recipe<C>> Optional<T> betterContent$selectPolymorphRecipe(
            final RecipeManager recipeManager,
            final RecipeType<T> recipeType,
            final C input,
        final Level level,
        final Player player
    ) {
        // TCon calculates an individual result for each viewer. Keep the selection on that
        // player, rather than on the station shared by every player who opens it.
        if (player == null) return recipeManager.getRecipeFor(recipeType, input, level);
        return RecipeSelection.getPlayerRecipe(player.containerMenu, recipeType, input, level, player);
    }
}
