package com.bettercontent.bettercompatfixes.compat.epicfight;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class StyleInventoryPolicyTest {
    @Test void onlyPersonalScreensWithEpicFightAreAdmitted() {
        assertTrue(StyleInventoryPolicy.accepts(true, "net.minecraft.client.gui.screens.inventory.InventoryScreen", true));
        for (String name : new String[]{"CuriosScreen", "CuriosScreenV2"}) {
            String type = "top.theillusivec4.curios.client.gui." + name;
            assertTrue(StyleInventoryPolicy.accepts(false, type, true));
            assertFalse(StyleInventoryPolicy.accepts(false, type, false));
            assertFalse(StyleInventoryPolicy.accepts(false, type + "Foreign", true));
        }
        assertFalse(StyleInventoryPolicy.accepts(true, "InventoryScreen", false));
        for (String type : new String[]{"net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen",
                "net.minecraft.client.gui.screens.inventory.ContainerScreen", "net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen"})
            assertFalse(StyleInventoryPolicy.accepts(false, type, true));
    }
    @Test void placementTracksNativeOriginAndStaysAboveFrame() {
        assertEquals(148, StyleInventoryPolicy.buttonX(100));
        assertEquals(48, StyleInventoryPolicy.buttonX(0));
        assertEquals(60, StyleInventoryPolicy.buttonY(80));
        assertEquals(2, StyleInventoryPolicy.buttonY(10));
        String source;
        try {
            source = Files.readString(Path.of("src/main/java/com/bettercontent/bettercompatfixes/compat/epicfight/StyleInventoryEntry.java"));
        } catch (java.io.IOException error) { throw new AssertionError(error); }
        assertTrue(source.contains("screen.getGuiLeft()"));
        assertTrue(source.contains("screen.getGuiTop()"));
        assertFalse(source.contains("screen.width - 176"));
        assertEquals(1, source.split("event.addListener", -1).length - 1);
        assertTrue(source.contains("new StyleScreen()"));
    }
}
