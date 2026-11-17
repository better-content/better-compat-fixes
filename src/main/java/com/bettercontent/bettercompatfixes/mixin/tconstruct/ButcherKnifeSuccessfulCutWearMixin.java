package com.bettercontent.bettercompatfixes.mixin.tconstruct;

import com.bettercontent.bettercompatfixes.compat.ButcherKnifeDurability;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/** Pinned TCon 3.11.2.166: replace native one-point shears damage only after a real cut.
 * Ordinary ItemStack.mineBlock follows successful vanilla removal; the AoE owner invokes
 * damageAnimated only when its own removeBlock returned true. Keep all native modifier hooks. */
@Pseudo
@Mixin(targets = "slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic", remap = false)
public abstract class ButcherKnifeSuccessfulCutWearMixin {
    private static final String NATIVE_DAMAGE = "Lslimeknights/tconstruct/library/tools/helper/ToolDamageUtil;"
            + "damageAnimated(Lslimeknights/tconstruct/library/tools/nbt/IToolStackView;"
            + "ILnet/minecraft/world/entity/LivingEntity;)Z";

    @WrapOperation(method = "mineBlock", at = @At(value = "INVOKE", target = NATIVE_DAMAGE, remap = false),
            require = 1, expect = 1, allow = 1, remap = false)
    private static boolean betterContentFixes$ordinarySuccessfulCut(IToolStackView nativeTool, int nativeDamage,
            LivingEntity miner, Operation<Boolean> original, @Local(argsOnly = true) ItemStack tool,
            @Local(argsOnly = true) BlockState cut) {
        int amount = miner instanceof ServerPlayer player
                ? ButcherKnifeDurability.nativeCutDamage(nativeDamage, tool, cut, player) : nativeDamage;
        return original.call(nativeTool, amount, miner);
    }

    @WrapOperation(method = "breakBlock(Lslimeknights/tconstruct/library/tools/nbt/IToolStackView;"
            + "Lnet/minecraft/world/item/ItemStack;Lslimeknights/tconstruct/library/tools/context/ToolHarvestContext;Z)Z",
            at = @At(value = "INVOKE", target = NATIVE_DAMAGE, remap = false),
            require = 1, expect = 1, allow = 1, remap = false)
    private static boolean betterContentFixes$aoeSuccessfulCut(IToolStackView nativeTool, int nativeDamage,
            LivingEntity miner, Operation<Boolean> original, @Local(argsOnly = true) ItemStack tool,
            @Local(argsOnly = true) ToolHarvestContext context) {
        int amount = ButcherKnifeDurability.nativeCutDamage(nativeDamage, tool, context.getState(),
                miner instanceof ServerPlayer player ? player : null);
        return original.call(nativeTool, amount, miner);
    }
}
