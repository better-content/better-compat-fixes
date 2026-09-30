package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.FluidMixBlocker;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class FluidMixBlockerGameTests {
    private FluidMixBlockerGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void cobblestoneGenerationIsReverted(final GameTestHelper helper) {
        final BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.setBlock(origin, Blocks.WATER.defaultBlockState());
        final BlockEvent.FluidPlaceBlockEvent event = new BlockEvent.FluidPlaceBlockEvent(
                helper.getLevel(),
                origin,
                origin.east(),
                Blocks.COBBLESTONE.defaultBlockState()
        );
        final BlockState expectedState = event.getOriginalState();

        FluidMixBlocker.onFluidPlaceBlock(event);
        if (event.getNewState() != expectedState) {
            helper.fail("Expected disallowed fluid-generated cobblestone to revert to "
                    + expectedState.getBlock().builtInRegistryHolder().key().location()
                    + " but found "
                    + event.getNewState().getBlock().builtInRegistryHolder().key().location());
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void obsidianGenerationRemainsAllowed(final GameTestHelper helper) {
        final BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.setBlock(origin, Blocks.LAVA.defaultBlockState());
        final BlockEvent.FluidPlaceBlockEvent event = new BlockEvent.FluidPlaceBlockEvent(
                helper.getLevel(),
                origin,
                origin.above(),
                Blocks.OBSIDIAN.defaultBlockState()
        );

        FluidMixBlocker.onFluidPlaceBlock(event);
        if (!event.getNewState().is(FluidMixBlocker.ALLOWED_FLUID_GENERATED_BLOCKS)) {
            helper.fail("Expected allowlisted obsidian generation to remain intact");
            return;
        }
        helper.succeed();
    }
}
