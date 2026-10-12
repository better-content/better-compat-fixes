package com.bettercontent.bettercompatfixes.client.quark;

/** Input arbitration only. Slot contents, ordering and packets remain native-owned. */
public final class NativeSortPolicy {
    public enum Target { NONE, PLAYER, CONTAINER_PLAYER, CONTAINER }
    private NativeSortPolicy() {}
    public static Target target(boolean blocked, boolean personal, boolean acceptedContainer,
                                boolean playerSlot, int inventoryIndex) {
        if (blocked) return Target.NONE;
        // Native player equipment/offhand are not inventory sorting surfaces.
        if (playerSlot) {
            if (inventoryIndex < 0 || inventoryIndex >= 36) return Target.NONE;
            return personal ? Target.PLAYER : acceptedContainer ? Target.CONTAINER_PLAYER : Target.NONE;
        }
        return !personal && acceptedContainer ? Target.CONTAINER : Target.NONE;
    }
    public static boolean contains(double mouseX, double mouseY, int slotX, int slotY) {
        return mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16;
    }
}
