package com.bettercontent.bettercompatfixes.mixin.dynamictrees;

import com.bettercontent.bettercompatfixes.compat.DynamicTreesFellingWear;
import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Consumer;

/** Pinned Forge mining charge, before BranchBlock queues its deferred FutureBreak. */
@Mixin(ItemStack.class)
abstract class FellingItemStackMixin {
    @WrapMethod(method = {"mineBlock", "m_41686_"}, remap = false)
    private void betterContent$scopeBranchMining(Level level, BlockState state, BlockPos pos,
                                                 Player player, Operation<Void> original) {
        ItemStack stack = (ItemStack) (Object) this;
        int multiplier = BcFixesConfig.dynamicTreesFellingDurabilityMultiplier();
        if (multiplier == 1 || level.isClientSide || !(player instanceof ServerPlayer)
                || player instanceof FakePlayer || !(state.getBlock() instanceof BranchBlock)
                || player.getMainHandItem() != stack || !stack.canPerformAction(ToolActions.AXE_DIG)) {
            original.call(level, state, pos, player);
            return;
        }
        try (var ignored = DynamicTreesFellingWear.enter(player, stack, multiplier)) {
            original.call(level, state, pos, player);
        }
    }

    @WrapMethod(method = {"hurtAndBreak", "m_41622_"}, remap = false)
    private <T extends LivingEntity> void betterContent$scaleNominalFellingWear(
            int amount, T entity, Consumer<T> onBreak, Operation<Void> original) {
        original.call(DynamicTreesFellingWear.scale(entity, this, amount), entity, onBreak);
    }
}
