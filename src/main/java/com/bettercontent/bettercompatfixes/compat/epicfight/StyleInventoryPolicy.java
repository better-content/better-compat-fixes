package com.bettercontent.bettercompatfixes.compat.epicfight;

/** Screen admission and placement only; no optional Curios class linkage. */
public final class StyleInventoryPolicy {
    private StyleInventoryPolicy() {}
    public static boolean accepts(boolean vanillaInventory, String screenClass, boolean epicFightLoaded) {
        return epicFightLoaded && (vanillaInventory
                || screenClass.equals("top.theillusivec4.curios.client.gui.CuriosScreen")
                || screenClass.equals("top.theillusivec4.curios.client.gui.CuriosScreenV2"));
    }
    public static int buttonX(int guiLeft) { return guiLeft + 48; }
    public static int buttonY(int guiTop) { return Math.max(2, guiTop - 20); }
}
