package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.ExtendedItemPickup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class ExtendedItemPickupGameTests {
    private ExtendedItemPickupGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void extraRadiusExpandsOnlyHorizontally(final GameTestHelper helper) {
        final var playerBounds = new AABB(2.2D, 2.0D, 2.2D, 2.8D, 3.8D, 2.8D);
        final var vanilla = ExtendedItemPickup.vanillaPickupBounds(playerBounds, null);
        final var extended = ExtendedItemPickup.extendHorizontally(vanilla, 1.0D);
        helper.assertTrue(extended.getYsize() == vanilla.getYsize(), "Vertical pickup reach must remain vanilla");
        helper.assertTrue(extended.getXsize() == vanilla.getXsize() + 2.0D,
                "One block must be added to both horizontal X edges");
        helper.assertTrue(extended.getZsize() == vanilla.getZsize() + 2.0D,
                "One block must be added to both horizontal Z edges");
        helper.assertTrue(!vanilla.contains(4.25D, 2.5D, 2.5D)
                        && extended.contains(4.25D, 2.5D, 2.5D),
                "The added shell must cover an ordinary item center outside vanilla reach");
        helper.succeed();
    }
}
