package com.bettercontent.bettercompatfixes.mixin.dynamictrees;

import com.bettercontent.bettercompatfixes.compat.DynamicTreesFellingWear;
import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.systems.nodemapper.NetVolumeNode;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BranchBlock.class, remap = false)
abstract class FellingBranchMixin {
    // Wrap only the felling call site. Stripping shares damageAxe but never enters this scope.
    @WrapOperation(method = "futureBreak", at = @At(value = "INVOKE",
            target = "Lcom/ferreusveritas/dynamictrees/block/branch/BranchBlock;damageAxe(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;ILcom/ferreusveritas/dynamictrees/systems/nodemapper/NetVolumeNode$Volume;Z)V"),
            require = 1, expect = 1, allow = 1)
    private void betterContent$scopeDeferredFelling(BranchBlock branch, LivingEntity entity,
            ItemStack stack, int radius, NetVolumeNode.Volume volume, boolean forBlockBreak,
            Operation<Void> original) {
        int multiplier = BcFixesConfig.dynamicTreesFellingDurabilityMultiplier();
        if (multiplier == 1 || !forBlockBreak || !(entity instanceof ServerPlayer player)
                || entity instanceof FakePlayer || player.getMainHandItem() != stack) {
            original.call(branch, entity, stack, radius, volume, forBlockBreak);
            return;
        }
        try (var ignored = DynamicTreesFellingWear.enter(player, stack, multiplier)) {
            original.call(branch, entity, stack, radius, volume, forBlockBreak);
        }
    }
}
