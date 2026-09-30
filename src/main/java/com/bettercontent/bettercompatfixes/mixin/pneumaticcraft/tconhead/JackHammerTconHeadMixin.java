package com.bettercontent.bettercompatfixes.mixin.pneumaticcraft.tconhead;

import com.bettercontent.bettercompatfixes.compat.pneumaticcraft.TconJackhammerHeadPolicy;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.desht.pneumaticcraft.common.item.DrillBitItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "me.desht.pneumaticcraft.common.item.JackHammerItem", remap = false)
abstract class JackHammerTconHeadMixin {
    @Inject(method = "getDrillBit", at = @At("HEAD"), cancellable = true, remap = false)
    private void betterContent$useTconHeadTier(final ItemStack jackhammer,
                                               final CallbackInfoReturnable<DrillBitItem.DrillBitType> cir) {
        final ItemStack head = TconJackhammerHeadPolicy.readInstalledHead(jackhammer);
        if (head.isEmpty()) return;
        cir.setReturnValue(TconJackhammerHeadPolicy.wornOut(head)
                ? DrillBitItem.DrillBitType.NONE
                : TconJackhammerHeadPolicy.drillBitType(head));
    }

    @ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"), remap = false)
    private float betterContent$scaleByMaterialSpeed(final float original, final ItemStack jackhammer) {
        final ItemStack head = TconJackhammerHeadPolicy.readInstalledHead(jackhammer);
        if (head.isEmpty() || TconJackhammerHeadPolicy.wornOut(head)) return original;
        final float materialSpeed = TconJackhammerHeadPolicy.profile(head)
                .map(stats -> stats.miningSpeed()).orElse(0f);
        return original * TconJackhammerHeadPolicy.miningSpeedScale(materialSpeed);
    }

    @ModifyConstant(
            method = "lambda$onBlockStartBreak$1",
            constant = @org.spongepowered.asm.mixin.injection.Constant(floatValue = 50.0f),
            require = 1,
            remap = false)
    private float betterContent$scalePerBlockAirCost(final float baseReservation, final ItemStack jackhammer) {
        final ItemStack head = TconJackhammerHeadPolicy.readInstalledHead(jackhammer);
        if (head.isEmpty() || TconJackhammerHeadPolicy.wornOut(head)) return baseReservation;
        final float materialSpeed = TconJackhammerHeadPolicy.profile(head)
                .map(stats -> stats.miningSpeed()).orElse(0f);
        return TconJackhammerHeadPolicy.scaledAirReservation((int) baseReservation, materialSpeed);
    }
}
