package com.bettercontent.bettercompatfixes.client;

import com.bettercontent.bettercompatfixes.compat.epicfight.StyleInventoryPolicy;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class JournalStyleEntryTest {
    @Test void journalHostsKeepRecordStancesInsteadOfDuplicatingTheCentralButton() {
        for (String type : new String[]{"top.theillusivec4.curios.client.gui.CuriosScreen", "top.theillusivec4.curios.client.gui.CuriosScreenV2"}) {
            assertTrue(StyleInventoryPolicy.accepts(false, type, true));
            assertTrue(StyleInventoryPolicy.journalHost(true, false, type));
            assertFalse(StyleInventoryPolicy.journalHost(false, false, type));
        }
        assertTrue(StyleInventoryPolicy.journalHost(true, true, "net.minecraft.client.gui.screens.inventory.InventoryScreen"));
        assertFalse(StyleInventoryPolicy.journalHost(false, true, "net.minecraft.client.gui.screens.inventory.InventoryScreen"));
        assertFalse(StyleInventoryPolicy.journalHost(true, false, "net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen"));
        assertFalse(StyleInventoryPolicy.journalHost(true, false, "net.minecraft.client.gui.screens.inventory.ContainerScreen"));
    }
    @Test void optionalJournalPresenceNeverLinksItsClasses() throws Exception {
        String entry = Files.readString(Path.of("src/main/java/com/bettercontent/bettercompatfixes/compat/epicfight/StyleInventoryEntry.java"));
        assertTrue(entry.contains("ModList.get().isLoaded(\"better_journal_inventory\")"));
        assertTrue(entry.contains("StyleInventoryPolicy.journalHost("));
        assertTrue(entry.indexOf("StyleInventoryPolicy.journalHost(") < entry.indexOf("event.addListener("));
        assertFalse(entry.contains("import com.bettercontent.betterjournalinventory"));
        assertFalse(entry.contains("Class.forName"));
    }
}
