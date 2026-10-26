package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.compat.epicfight.OptionalTinkerSkinsMixinPlugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class OptionalTinkerSkinsGateTest {
    private static final String TARGET = "yesman.epicfight.api.client.model.ItemSkinsReloadListener";
    private static final String MIXIN = "com.bettercontent.bettercompatfixes.mixin.epicfight.OptionalTinkerItemSkinsMixin";

    @Test void selectsOnlyExactPairAndBothExactVersions() {
        assertTrue(OptionalTinkerSkinsMixinPlugin.selects(TARGET, MIXIN, "20.14.17", "20.2.8"));
        for (String version : new String[]{null, "", "20.14.16", "20.14.17-extra"})
            assertFalse(OptionalTinkerSkinsMixinPlugin.selects(TARGET, MIXIN, version, "20.2.8"));
        for (String version : new String[]{null, "", "20.2.7", "20.2.8-extra"})
            assertFalse(OptionalTinkerSkinsMixinPlugin.selects(TARGET, MIXIN, "20.14.17", version));
        assertFalse(OptionalTinkerSkinsMixinPlugin.selects(TARGET, MIXIN, null, null));
    }

    @Test void unknownNullOrNearMatchTargetsCannotActivateTheHook() {
        for (String target : new String[]{null, "", TARGET + "Extra", "other.ItemSkinsReloadListener"})
            assertFalse(OptionalTinkerSkinsMixinPlugin.selects(target, MIXIN, "20.14.17", "20.2.8"));
        for (String mixin : new String[]{null, "", MIXIN + "Extra", "other.OptionalTinkerItemSkinsMixin"})
            assertFalse(OptionalTinkerSkinsMixinPlugin.selects(TARGET, mixin, "20.14.17", "20.2.8"));
    }

    public static void main(String[] args) {
        var test = new OptionalTinkerSkinsGateTest();
        test.selectsOnlyExactPairAndBothExactVersions();
        test.unknownNullOrNearMatchTargetsCannotActivateTheHook();
    }
}
