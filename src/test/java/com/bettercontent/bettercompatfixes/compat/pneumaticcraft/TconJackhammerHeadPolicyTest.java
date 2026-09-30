package com.bettercontent.bettercompatfixes.compat.pneumaticcraft;

import me.desht.pneumaticcraft.common.item.DrillBitItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TconJackhammerHeadPolicyTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError error) {
            final boolean expectedForgeHarnessFailure = java.util.stream.Stream.iterate(
                            (Throwable) error, java.util.Objects::nonNull, Throwable::getCause)
                    .anyMatch(cause -> cause instanceof NoSuchMethodException
                            && cause.getMessage() != null
                            && cause.getMessage().startsWith("net.minecraftforge.network.NetworkEvent"));
            if (!expectedForgeHarnessFailure) {
                throw error;
            }
        }
    }

    @Test
    void persistedMaterialIdentityIsLimitedToTheActualPickHeadStat() {
        assertTrue(TconJackhammerHeadPolicy.supportsPersistedIdentity(
                "tconstruct:pick_head", "tconstruct:head", "tconstruct:iron"));
        assertFalse(TconJackhammerHeadPolicy.supportsPersistedIdentity(
                "tconstruct:hammer_head", "tconstruct:head", "tconstruct:iron"));
        assertFalse(TconJackhammerHeadPolicy.supportsPersistedIdentity(
                "tconstruct:pick_head", "tconstruct:handle", "tconstruct:iron"));
        assertFalse(TconJackhammerHeadPolicy.supportsPersistedIdentity(
                "tconstruct:pick_head", "tconstruct:head", ""));
    }

    @Test
    void harvestLevelSelectsNativeJackhammerTierAndItsModeCeiling() {
        assertEquals(DrillBitItem.DrillBitType.IRON, TconJackhammerHeadPolicy.drillBitTypeForLevel(0));
        assertEquals(DrillBitItem.DrillBitType.COMPRESSED_IRON, TconJackhammerHeadPolicy.drillBitTypeForLevel(2));
        assertEquals(DrillBitItem.DrillBitType.DIAMOND, TconJackhammerHeadPolicy.drillBitTypeForLevel(3));
        assertEquals(DrillBitItem.DrillBitType.NETHERITE, TconJackhammerHeadPolicy.drillBitTypeForLevel(4));
        assertEquals(DrillBitItem.DrillBitType.NONE, TconJackhammerHeadPolicy.drillBitTypeForLevel(-1));
    }

    @Test
    void wearBelongsToEachPersistedPhysicalHeadAndCannotBeResetBySwapping() {
        CompoundTag wornIron = new CompoundTag();
        CompoundTag freshDiamond = new CompoundTag();

        assertEquals(1, TconJackhammerHeadPolicy.advanceWear(wornIron, 10));
        for (int block = 1; block < 7; block++) {
            assertEquals(block + 1, TconJackhammerHeadPolicy.advanceWear(wornIron, 10));
        }
        assertEquals(7, TconJackhammerHeadPolicy.readWear(wornIron));
        assertEquals(0, TconJackhammerHeadPolicy.readWear(freshDiamond));
        assertEquals(8, TconJackhammerHeadPolicy.advanceWear(wornIron, 10));
        assertEquals(9, TconJackhammerHeadPolicy.advanceWear(wornIron, 10));
        assertEquals(10, TconJackhammerHeadPolicy.advanceWear(wornIron, 10));
        assertEquals(10, TconJackhammerHeadPolicy.advanceWear(wornIron, 10));
        assertEquals(0, TconJackhammerHeadPolicy.advanceWear(freshDiamond, 0));
    }

    @Test
    void materialSpeedChangesBothWorkRateAndAirReservationWithinBounds() {
        assertEquals(0.5f, TconJackhammerHeadPolicy.miningSpeedScale(3f));
        assertEquals(1f, TconJackhammerHeadPolicy.miningSpeedScale(6f));
        assertEquals(1.5f, TconJackhammerHeadPolicy.miningSpeedScale(12f));
        assertEquals(25, TconJackhammerHeadPolicy.scaledAirReservation(50, 3f));
        assertEquals(50, TconJackhammerHeadPolicy.scaledAirReservation(50, 6f));
        assertEquals(75, TconJackhammerHeadPolicy.scaledAirReservation(50, 12f));
        assertEquals(50, TconJackhammerHeadPolicy.scaledAirReservation(50, Float.NaN));
    }
}
