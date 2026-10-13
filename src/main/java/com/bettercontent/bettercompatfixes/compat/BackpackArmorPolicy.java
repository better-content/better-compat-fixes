package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;

/** Curios-free recognition/admission support. Optional Curios absence never enables armor bags. */
public final class BackpackArmorPolicy {
    private BackpackArmorPolicy() {}

    public static boolean backpack(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem;
    }

    public static boolean hasArmorBackpack(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR && backpack(player.getItemBySlot(slot))) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // With Curios present, the combined equipment warning scans both owners exactly once.
        if (!ModList.get().isLoaded("curios") && hasArmorBackpack(event.getEntity())) {
            event.getEntity().sendSystemMessage(Component.translatable("better_compat_fixes.backpack.unavailable"));
        }
    }
}
