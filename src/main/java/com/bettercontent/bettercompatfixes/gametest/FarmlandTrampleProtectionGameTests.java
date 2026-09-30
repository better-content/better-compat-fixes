package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.FarmlandTrampleProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class FarmlandTrampleProtectionGameTests {
    private FarmlandTrampleProtectionGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void farmlandTrampleIsCanceled(final GameTestHelper helper) {
        final BlockPos farmlandPos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.setBlock(farmlandPos, Blocks.FARMLAND.defaultBlockState());
        final FallingBlockEntity entity = new FallingBlockEntity(EntityType.FALLING_BLOCK, helper.getLevel());
        final BlockEvent.FarmlandTrampleEvent event = new BlockEvent.FarmlandTrampleEvent(
                helper.getLevel(),
                farmlandPos,
                Blocks.FARMLAND.defaultBlockState(),
                1.5f,
                entity
        );

        FarmlandTrampleProtection.onFarmlandTrample(event);
        if (!event.isCanceled()) {
            helper.fail("Expected farmland trample to be canceled");
            return;
        }
        helper.succeed();
    }
}
