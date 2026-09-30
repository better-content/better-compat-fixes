package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.DaylightProtectionPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class DaylightProtectionGameTests {
    private DaylightProtectionGameTests() {
    }

    @GameTest(
            templateNamespace = BetterContentFixes.MOD_ID,
            template = "daylight_platform",
            batch = "better_compat_fixes_daylight",
            timeoutTicks = 400)
    public static void zombiesDoNotIgniteUnderOpenSky(final GameTestHelper helper) {
        configureClearWeather(helper, 6000L);
        final Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        validateMobStaysOutOfSunBurn(helper, zombie, "zombie");
    }

    @GameTest(
            templateNamespace = BetterContentFixes.MOD_ID,
            template = "daylight_platform",
            batch = "better_compat_fixes_daylight",
            timeoutTicks = 400)
    public static void skeletonsDoNotIgniteUnderOpenSky(final GameTestHelper helper) {
        configureClearWeather(helper, 6000L);
        final Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, new BlockPos(2, 1, 2));
        validateMobStaysOutOfSunBurn(helper, skeleton, "skeleton");
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "daylight_platform")
    public static void phantomsRetainVanillaDaylightBurning(final GameTestHelper helper) {
        final Phantom phantom = helper.spawnWithNoFreeWill(EntityType.PHANTOM, new BlockPos(2, 1, 2));
        helper.assertTrue(
                !DaylightProtectionPolicy.disablesSunBurnTick(phantom),
                "Phantoms must retain their vanilla daylight-burn check");
        helper.succeed();
    }

    @GameTest(
            templateNamespace = BetterContentFixes.MOD_ID,
            template = "daylight_platform",
            batch = "better_compat_fixes_ordinary_fire",
            timeoutTicks = 200)
    public static void ordinaryFireStillIgnitesProtectedMobs(final GameTestHelper helper) {
        configureClearWeather(helper, 18000L);
        final Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        helper.assertTrue(
                DaylightProtectionPolicy.disablesSunBurnTick(zombie),
                "The configured daylight policy must protect zombies");
        zombie.setSecondsOnFire(8);
        helper.runAfterDelay(20, () -> {
            if (!zombie.isOnFire()) {
                helper.fail("Expected ordinary non-solar fire to keep burning the protected zombie");
                return;
            }
            helper.succeed();
        });
    }

    private static void validateMobStaysOutOfSunBurn(final GameTestHelper helper, final Mob mob, final String label) {
        helper.assertTrue(
                DaylightProtectionPolicy.disablesSunBurnTick(mob),
                "The configured daylight policy must protect the " + label);
        mob.clearFire();
        helper.runAfterDelay(200, () -> {
            if (mob.isOnFire()) {
                helper.fail("Expected " + label + " to remain unlit under daylight protection");
                return;
            }
            helper.succeed();
        });
    }

    private static void configureClearWeather(final GameTestHelper helper, final long dayTime) {
        helper.getLevel().setDayTime(dayTime);
        helper.getLevel().setWeatherParameters(0, 0, false, false);
    }
}
