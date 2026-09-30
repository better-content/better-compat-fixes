package com.bettercontent.bettercompatfixes.compat.epicfight;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Pure identity and persistence policy for styles observed at Epic Fight attack start. */
public final class StyleDiscoveryPolicy {
    private StyleDiscoveryPolicy() {
    }

    public static String key(final ResourceLocation itemId, final String styleId) {
        if (itemId == null || styleId == null || styleId.isBlank()) {
            return null;
        }
        return itemId + "|" + styleId;
    }

    public static boolean isNew(final String key, final Iterable<String> discovered) {
        if (key == null) {
            return false;
        }
        for (final String entry : discovered) {
            if (Objects.equals(key, entry)) {
                return false;
            }
        }
        return true;
    }
}
