package com.bettercontent.bettercompatfixes.mixin.createsifter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps rolled Sifter products when the internal output inventory fills mid-cycle. */
@Mixin(targets = "com.oierbravo.createsifter.content.contraptions.components.sifter.SifterBlockEntity", remap = false)
public abstract class SifterBlockEntityMixin {
    @Inject(method = "tryToInsertOutputItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void betterContent$preserveOutputRemainder(
            final ItemStackHandler output,
            final ItemStack stack,
            final boolean simulate,
            final CallbackInfo callback
    ) {
        final ItemStack remainder = ItemHandlerHelper.insertItemStacked(output, stack, simulate);
        if (!simulate && !remainder.isEmpty()) {
            final BlockEntity self = (BlockEntity) (Object) this;
            final Level level = self.getLevel();
            final BlockPos pos = self.getBlockPos();
            if (level != null) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, remainder);
            }
        }
        callback.cancel();
    }
}
