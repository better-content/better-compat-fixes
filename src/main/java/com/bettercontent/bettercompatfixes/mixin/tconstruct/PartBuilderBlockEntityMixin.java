package com.bettercontent.bettercompatfixes.mixin.tconstruct;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity;
import com.bettercontent.bettercompatfixes.tconstruct.FreehandPattern;

/** Routes recipe discovery and consumption through the Part Builder's single visible input. */
@Mixin(value = PartBuilderBlockEntity.class, remap = false)
public abstract class PartBuilderBlockEntityMixin {
    @ModifyExpressionValue(
        method = "getCurrentRecipes",
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lslimeknights/tconstruct/tables/block/entity/table/PartBuilderBlockEntity;m_8020_(I)Lnet/minecraft/world/item/ItemStack;",
            remap = false
        )
    )
    private ItemStack better_compat_fixes$allowFreehandRecipeDiscovery(ItemStack patternStack) {
        return better_compat_fixes$resolveFreehandPattern(patternStack);
    }

    @ModifyExpressionValue(
        method = "getCurrentRecipes",
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lslimeknights/tconstruct/tables/block/entity/table/PartBuilderBlockEntity;getItem(I)Lnet/minecraft/world/item/ItemStack;",
            remap = false
        )
    )
    private ItemStack better_compat_fixes$allowFreehandRecipeDiscoveryInDevelopment(ItemStack patternStack) {
        return better_compat_fixes$resolveFreehandPattern(patternStack);
    }

    private ItemStack better_compat_fixes$resolveFreehandPattern(ItemStack patternStack) {
        PartBuilderBlockEntity builder = (PartBuilderBlockEntity) (Object) this;
        return FreehandPattern.resolvePattern(patternStack, builder.getItem(0));
    }

    @ModifyArg(
        method = "onCraft",
        require = 1,
        index = 0,
        at = @At(
            value = "INVOKE",
            target = "Lslimeknights/tconstruct/tables/block/entity/table/PartBuilderBlockEntity;shrinkSlot(IILnet/minecraft/world/entity/player/Player;)V",
            ordinal = 1,
            remap = false
        )
    )
    private int better_compat_fixes$consumeVisibleCastPattern(int patternSlot) {
        PartBuilderBlockEntity builder = (PartBuilderBlockEntity) (Object) this;
        return FreehandPattern.usesVisibleCastPattern(builder.getItem(1), builder.getItem(0)) ? 0 : patternSlot;
    }
}
