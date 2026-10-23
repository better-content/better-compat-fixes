package com.bettercontent.bettercompatfixes.compat.dynamictrees;

import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import com.ferreusveritas.dynamictrees.block.rooty.SoilHelper;
import net.minecraft.world.level.block.state.BlockState;

/** Reads only the already-sampled final root state and the native soil registry metadata. */
public final class RootySoilAdmission {
    private RootySoilAdmission() {}
    public static boolean reject(boolean worldGen, BlockState initialDirtState) {
        if (!worldGen) return false;
        var block = initialDirtState.getBlock();
        return RootySoilPolicy.reject(true, SoilHelper.isSoilRegistered(block), block instanceof RootyBlock);
    }
}
