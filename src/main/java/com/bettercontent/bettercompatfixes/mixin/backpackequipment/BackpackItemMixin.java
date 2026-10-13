package com.bettercontent.bettercompatfixes.mixin.backpackequipment;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import org.spongepowered.asm.mixin.Mixin;

/** Override the inherited Forge armor-admission default, not the bag's native use/place/open. */
@Mixin(value = BackpackItem.class, remap = false)
public abstract class BackpackItemMixin {
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, Entity entity) {
        if (entity instanceof Player && slot.getType() == EquipmentSlot.Type.ARMOR) return false;
        // Exact Forge IForgeItem default for mobs and non-armor slots (including hands).
        return Mob.getEquipmentSlotForItem(stack) == slot;
    }
}
