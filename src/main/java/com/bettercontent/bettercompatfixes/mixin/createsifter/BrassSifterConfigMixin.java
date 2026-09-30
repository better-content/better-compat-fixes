package com.bettercontent.bettercompatfixes.mixin.createsifter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Gives the Brass Sifter's items-per-cycle setting its own Forge config key. */
@Mixin(targets = "com.oierbravo.createsifter.content.contraptions.components.brasss_sifter.BrassSifterConfig", remap = false)
public abstract class BrassSifterConfigMixin {
    @ModifyArg(
            method = "registerCommonConfig",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/common/ForgeConfigSpec$Builder;defineInRange(Ljava/lang/String;III)Lnet/minecraftforge/common/ForgeConfigSpec$IntValue;",
                    ordinal = 0
            ),
            index = 1,
            remap = false
    )
    private static int betterContent$boundOutputSlots(final int original) {
        return 16;
    }

    @ModifyArg(
            method = "registerCommonConfig",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/common/ForgeConfigSpec$Builder;defineInRange(Ljava/lang/String;III)Lnet/minecraftforge/common/ForgeConfigSpec$IntValue;",
                    ordinal = 0
            ),
            index = 2,
            remap = false
    )
    private static int betterContent$requireOutputSlots(final int original) {
        return 16;
    }

    @ModifyArg(
            method = "registerCommonConfig",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/common/ForgeConfigSpec$Builder;defineInRange(Ljava/lang/String;III)Lnet/minecraftforge/common/ForgeConfigSpec$IntValue;",
                    ordinal = 1
            ),
            index = 0,
            remap = false
    )
    private static String betterContent$separateItemsPerCycleKey(final String original) {
        return "itemsPerCycle";
    }
}
