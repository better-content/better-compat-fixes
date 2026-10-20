package com.bettercontent.bettercompatfixes.compat.epicfight;

/** Screen admission and placement only; no optional Curios class linkage. */
public final class StyleInventoryPolicy {
    private StyleInventoryPolicy() {}
    public static boolean accepts(boolean vanillaInventory, String screenClass, boolean epicFightLoaded) {
        return epicFightLoaded && (vanillaInventory
                || screenClass.equals("top.theillusivec4.curios.client.gui.CuriosScreen")
                || screenClass.equals("top.theillusivec4.curios.client.gui.CuriosScreenV2"));
    }
    /** Mirrors Journal's screen-host admission without linking optional Journal classes. */
    public static boolean journalHost(boolean journalLoaded, boolean vanillaInventory, String screenClass) {
        return journalLoaded && (vanillaInventory
            || screenClass.startsWith("top.theillusivec4.curios.client.gui.CuriosScreen"));
    }
    public static int buttonX(int guiLeft) { return guiLeft + 48; }
    public static int buttonY(int guiTop) { return Math.max(2, guiTop - 20); }
}
