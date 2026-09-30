package com.bettercontent.bettercompatfixes.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class DtaetherTagCompatTest {
    @Test
    void makesOnlyTheRemovedImbuedSkyrootBranchOptional() {
        assertTrue(DtaetherTagCompat.shouldTreatAsOptional(id("dtaether", "imbued_skyroot_branch")));
        assertFalse(DtaetherTagCompat.shouldTreatAsOptional(id("dtaether", "skyroot_branch")));
        assertFalse(DtaetherTagCompat.shouldTreatAsOptional(id("dynamictrees", "imbued_skyroot_branch")));
    }

    private static ResourceLocation id(final String namespace, final String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
