package com.bettercontent.bettercompatfixes.mixin.tconstruct;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.tables.block.entity.inventory.PartBuilderContainerWrapper;
import slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity;
import com.bettercontent.bettercompatfixes.tconstruct.FreehandPattern;

/** Supplies the routed visible input while recipes match and assemble. */
@Mixin(value = PartBuilderContainerWrapper.class, remap = false)
public abstract class PartBuilderContainerWrapperMixin {
    @Shadow @Final private PartBuilderBlockEntity builder;

    @Inject(method = "getStack", at = @At("RETURN"), cancellable = true, require = 1)
    private void better_compat_fixes$routeVisibleMaterial(CallbackInfoReturnable<ItemStack> callback) {
        callback.setReturnValue(FreehandPattern.resolveMaterial(builder.getItem(1), callback.getReturnValue()));
    }

    @Inject(method = "getPatternStack", at = @At("RETURN"), cancellable = true, require = 1)
    private void better_compat_fixes$allowFreehandRecipeMatching(CallbackInfoReturnable<ItemStack> callback) {
        callback.setReturnValue(FreehandPattern.resolvePattern(callback.getReturnValue(), builder.getItem(0)));
    }
}
