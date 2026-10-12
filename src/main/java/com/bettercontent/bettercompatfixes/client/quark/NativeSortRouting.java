package com.bettercontent.bettercompatfixes.client.quark;

import com.bettercontent.bettercompatfixes.mixin.quark.SortProviderAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import org.violetmoon.quark.base.client.handler.InventoryButtonHandler.ButtonTargetType;
import org.violetmoon.quark.base.config.QuarkGeneralConfig;
import org.violetmoon.quark.base.handler.InventoryTransferHandler;
import org.violetmoon.zeta.client.event.play.ZScreen;
import com.google.common.collect.Multimap;

/** Select at most one existing Quark sort callback. No custom packets or storage access. */
public final class NativeSortRouting {
    private NativeSortRouting() {}
    private static boolean sorting(KeyMapping key) {
        return key != null && (key.getName().equals("quark.keybind.sort_player")
                || key.getName().equals("quark.keybind.sort_container"));
    }
    private static boolean matches(KeyMapping key, int button) {
        return key != null && key.matchesMouse(button) && (key.getKeyModifier() == KeyModifier.NONE
                || key.getKeyModifier().isActive(KeyConflictContext.GUI));
    }
    public static boolean ownsMiddleBinding(Multimap<ButtonTargetType, ?> providers) {
        return providers.values().stream().map(provider -> (SortProviderAccessor) provider)
                .anyMatch(provider -> sorting(provider.bc$keybind()) && matches(provider.bc$keybind(), 2));
    }
    public static boolean sophisticated(Screen screen) {
        // Short-circuit before resolving the optional native screen type when its mod is absent.
        return ModList.get().isLoaded("sophisticatedcore") && screen instanceof StorageScreenBase<?>;
    }
    private static boolean personal(Screen screen) {
        String name = screen.getClass().getName();
        return screen instanceof InventoryScreen
                || name.equals("top.theillusivec4.curios.client.gui.CuriosScreen")
                || name.equals("top.theillusivec4.curios.client.gui.CuriosScreenV2");
    }
    public static void route(ZScreen.MouseButtonPressed.Pre event, Multimap<ButtonTargetType, ?> providers) {
        Screen raw = event.getScreen();
        var mc = Minecraft.getInstance();
        if (event.isCanceled() || !(raw instanceof AbstractContainerScreen<?> screen)
                || raw instanceof CreativeModeInventoryScreen || sophisticated(raw) || mc.player == null
                || !QuarkGeneralConfig.isScreenAllowed(raw) || !screen.getMenu().getCarried().isEmpty()) return;
        Slot slot = screen.getSlotUnderMouse();
        // Use native hovered-slot authority, but reject stale hover and overlaid native controls.
        if (slot == null || !slot.isActive() || !screen.getMenu().slots.contains(slot)
                || !NativeSortPolicy.contains(event.getMouseX(), event.getMouseY(),
                    screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y)
                || screen.getChildAt(event.getMouseX(), event.getMouseY()).isPresent()) return;
        boolean own = personal(raw);
        var target = NativeSortPolicy.target(false, own, !own && InventoryTransferHandler.accepts(screen.getMenu(), mc.player),
                slot.container == mc.player.getInventory(), slot.getSlotIndex());
        ButtonTargetType nativeTarget = switch (target) {
            case PLAYER -> ButtonTargetType.PLAYER_INVENTORY;
            case CONTAINER_PLAYER -> ButtonTargetType.CONTAINER_PLAYER_INVENTORY;
            case CONTAINER -> ButtonTargetType.CONTAINER_INVENTORY;
            case NONE -> null;
        };
        if (nativeTarget == null) return;
        for (Object rawProvider : providers.get(nativeTarget)) {
            var provider = (SortProviderAccessor) rawProvider;
            if (!sorting(provider.bc$keybind()) || !matches(provider.bc$keybind(), event.getButton())
                    || !provider.bc$module().isEnabled() || !provider.bc$enableCond().getAsBoolean()) continue;
            provider.bc$pressed().accept(screen);
            event.setCanceled(true);
            return; // Never continue through Quark's HashSet dispatch after one sort.
        }
    }
}
