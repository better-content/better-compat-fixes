package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.resources.ResourceLocation;

public final class DtaetherTagCompat {
    private static final ResourceLocation OBSOLETE_IMBUED_SKYROOT_BRANCH =
            ResourceLocation.fromNamespaceAndPath("dtaether", "imbued_skyroot_branch");

    private DtaetherTagCompat() {
    }

    public static boolean shouldTreatAsOptional(final ResourceLocation id) {
        return OBSOLETE_IMBUED_SKYROOT_BRANCH.equals(id);
    }
}
