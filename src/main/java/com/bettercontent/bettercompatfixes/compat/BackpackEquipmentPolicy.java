package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/** Admission only: never mutates equipment, storage, saved stacks or command authoring. */
public final class BackpackEquipmentPolicy {
    private BackpackEquipmentPolicy() {}

    public static boolean backpack(ItemStack stack) {
        return BackpackArmorPolicy.backpack(stack);
    }

    public static boolean exclusiveSlot(String identifier, int index, boolean cosmetic) {
        return identifier.equals("better_backpack") && index == 0 && !cosmetic;
    }

    public static boolean applies(SlotContext context, ItemStack stack) {
        return context.entity() instanceof Player
                && (backpack(stack) || context.identifier().equals("better_backpack"));
    }

    public static boolean allowed(SlotContext context, ItemStack stack) {
        return backpack(stack) && exclusiveSlot(context.identifier(), context.index(), context.cosmetic());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (hasMisplacedBackpack(event.getEntity())) {
            event.getEntity().sendSystemMessage(Component.translatable("better_compat_fixes.backpack.misplaced"));
        }
    }

    public static boolean hasMisplacedBackpack(Player player) {
        if (BackpackArmorPolicy.hasArmorBackpack(player)) return true;
        return CuriosApi.getCuriosInventory(player).resolve().map(inventory ->
                inventory.getCurios().entrySet().stream().anyMatch(entry ->
                        misplaced(entry.getKey(), entry.getValue().getStacks(), false)
                                || misplaced(entry.getKey(), entry.getValue().getCosmeticStacks(), true)))
                .orElse(false);
    }

    private static boolean misplaced(String identifier, IDynamicStackHandler stacks, boolean cosmetic) {
        for (int index = 0; index < stacks.getSlots(); index++) {
            if (backpack(stacks.getStackInSlot(index)) && !exclusiveSlot(identifier, index, cosmetic)) return true;
        }
        return false;
    }
}
