package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class FluidMixBlocker {
    public static final TagKey<Block> ALLOWED_FLUID_GENERATED_BLOCKS =
            BlockTags.create(new ResourceLocation(BetterContentFixes.MOD_ID, "allowed_fluid_generated_blocks"));

    private FluidMixBlocker() {
    }

    @SubscribeEvent
    public static void onFluidPlaceBlock(final BlockEvent.FluidPlaceBlockEvent event) {
        if (!BcFixesConfig.fluidMixingBlockGeneratedBlocks()) {
            return;
        }

        final BlockState newState = event.getNewState();
        if (!newState.is(ALLOWED_FLUID_GENERATED_BLOCKS)) {
            event.setNewState(event.getOriginalState());
        }
    }
}
