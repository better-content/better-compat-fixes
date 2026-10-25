package com.bettercontent.bettercompatfixes.compat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class ClientReloadCompatibilityTest {
    @Test void onlyTheThreeExactBrokenEyePathsChange() {
        for (String dragon : List.of("fire", "ice", "lightning")) {
            String base = "assets/iceandfire/models/tabula/" + dragon + "dragon/" + dragon + "dragon";
            assertEquals(base + "_ground.tbl", DragonEyesModelPaths.classLoaderPath("/" + base + "_Ground.tbl"));
            for (String other : List.of(base + "_ground.tbl", "/" + base + "_ground.tbl",
                    "/" + base + "_Ground", "/" + base + "_Ground.tbl.extra"))
                assertEquals(other, DragonEyesModelPaths.classLoaderPath(other));
        }
        assertEquals("/assets/other/Ground.tbl", DragonEyesModelPaths.classLoaderPath("/assets/other/Ground.tbl"));
    }

    @Test void optionalSkinsAreSkippedOnlyWhenTheirExactItemsAreAbsent() {
        for (String item : List.of("constructs_casting:battlestaff", "constructs_casting:flamberge")) {
            assertTrue(OptionalTinkerSkinPolicy.skipSyntheticSkin(item, id -> false));
            assertFalse(OptionalTinkerSkinPolicy.skipSyntheticSkin(item, id -> true));
        }
        List<String> probes = new ArrayList<>();
        for (String item : List.of("cleaver", "tconstruct:cleaver", "tinkers_katanas:katana",
                "constructs_casting:other", "other:flamberge", "constructs_casting:flamberge_extra"))
            assertFalse(OptionalTinkerSkinPolicy.skipSyntheticSkin(item, id -> { probes.add(id); return false; }));
        assertTrue(probes.isEmpty(), "Unrelated defaults must not even consult the new registry guard");
        assertThrows(IllegalStateException.class, () -> OptionalTinkerSkinPolicy.skipSyntheticSkin(
                "constructs_casting:flamberge", id -> { throw new IllegalStateException("registry failed"); }));
    }

    public static void main(String[] args) {
        var test = new ClientReloadCompatibilityTest();
        test.onlyTheThreeExactBrokenEyePathsChange();
        test.optionalSkinsAreSkippedOnlyWhenTheirExactItemsAreAbsent();
    }
}
