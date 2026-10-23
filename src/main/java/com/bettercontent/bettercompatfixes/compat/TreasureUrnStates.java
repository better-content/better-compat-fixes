package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.world.level.block.state.BlockState;
import net.mehvahdjukaar.supplementaries.common.block.blocks.UrnBlock;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;

/** Linked only by the version/byte-pinned optional urn hooks. */
public final class TreasureUrnStates {
    private TreasureUrnStates() { }
    public static boolean isStatelessTreasure(BlockState state) {
        return state.getBlock() instanceof UrnBlock && state.is(ModRegistry.URN.get())
                && state.getValue(UrnBlock.TREASURE);
    }
}
