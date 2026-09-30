package com.bettercontent.bettercompatfixes.mixin.dynamictrees;

import com.bettercontent.bettercompatfixes.compat.DynamicTreesBranchHardnessPolicy;
import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.block.BlockWithDynamicHardness;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockWithDynamicHardness.class, remap = false)
abstract class BranchHardnessMixin {
    @Inject(method = "getHardness", at = @At("HEAD"), cancellable = true, remap = false)
    private void betterContent$inheritPrimitiveLogHardness(
            final BlockState state,
            final BlockGetter level,
            final BlockPos pos,
            final CallbackInfoReturnable<Float> cir
    ) {
        if (!BcFixesConfig.dynamicTreesSpeciesHardness()
                || !(state.getBlock() instanceof BranchBlock branch)) {
            return;
        }

        final Family family = branch.getFamily(state, level, pos);
        if (family == null) {
            return;
        }

        final Block primitiveLog = family.getPrimitiveLog().orElse(null);
        if (primitiveLog == null) {
            return;
        }

        final float nativeHardness = primitiveLog.defaultBlockState().getDestroySpeed(level, pos);
        cir.setReturnValue(DynamicTreesBranchHardnessPolicy.resolve(nativeHardness));
    }
}
