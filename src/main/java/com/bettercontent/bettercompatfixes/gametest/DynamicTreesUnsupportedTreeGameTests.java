package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.DynamicTreesSupportSweep;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class DynamicTreesUnsupportedTreeGameTests {
    private DynamicTreesUnsupportedTreeGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", timeoutTicks = 1200)
    public static void allRegisteredSpeciesLoseSupport(final GameTestHelper helper) {
        final var failures = DynamicTreesSupportSweep.run(
                helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)));
        if (!failures.isEmpty()) {
            helper.fail(String.join("; ", failures));
            return;
        }
        helper.succeed();
    }
}
