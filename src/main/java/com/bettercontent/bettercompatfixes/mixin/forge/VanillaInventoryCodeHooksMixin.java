package com.bettercontent.bettercompatfixes.mixin.forge;

import com.bettercontent.bettercompatfixes.compat.SophisticatedBarrelHopperSource;
import com.bettercontent.bettercompatfixes.compat.SophisticatedBarrelHopperTransfer;
import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraftforge.items.VanillaInventoryCodeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = VanillaInventoryCodeHooks.class, remap = false)
public abstract class VanillaInventoryCodeHooksMixin {
    @Inject(method = "extractHook", at = @At("HEAD"), cancellable = true)
    private static void betterContent$extractFromSophisticatedBarrel(
            final Level level,
            final Hopper hopper,
            final CallbackInfoReturnable<Boolean> cir
    ) {
        if (!BcFixesConfig.sophisticatedStorageBarrelHopperExtraction()) {
            return;
        }

        final BlockPos sourcePos = BlockPos.containing(
                hopper.getLevelX(),
                hopper.getLevelY() + 1.0D,
                hopper.getLevelZ());
        final BlockEntity sourceBlockEntity = level.getBlockEntity(sourcePos);
        if (!(sourceBlockEntity instanceof SophisticatedBarrelHopperSource source)
                || !source.betterContent$isBarrel()) {
            return;
        }

        cir.setReturnValue(SophisticatedBarrelHopperTransfer.tryMoveOneItem(
                source.betterContent$getInventoryForInputOutput(),
                hopper));
    }
}
