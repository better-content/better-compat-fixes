package com.bettercontent.bettercompatfixes.compat;

import java.util.ArrayList;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

/** Removes disabled vanilla potion-delivery stacks from newly placed structure data. */
public final class PotionAcquisitionPolicy {
    private PotionAcquisitionPolicy() {
    }

    public static boolean isDisabledPotionDelivery(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION)
                || stack.is(Items.TIPPED_ARROW)) {
            return true;
        }
        return stack.is(Items.POTION)
                && (PotionUtils.getPotion(stack) != Potions.WATER
                || !PotionUtils.getCustomEffects(stack).isEmpty());
    }

    /** Returns a sanitized copy; cached structure-template NBT is never mutated. */
    public static CompoundTag sanitizeBlockEntityNbt(final CompoundTag source) {
        if (source == null) {
            return null;
        }
        final CompoundTag sanitized = source.copy();
        sanitizeCompound(sanitized);
        return sanitized;
    }

    private static void sanitizeCompound(final CompoundTag compound) {
        for (final String key : new ArrayList<>(compound.getAllKeys())) {
            final Tag child = compound.get(key);
            if (child instanceof CompoundTag childCompound) {
                if (isSerializedDisabledStack(childCompound)) {
                    compound.remove(key);
                } else {
                    sanitizeCompound(childCompound);
                }
            } else if (child instanceof ListTag childList) {
                sanitizeList(childList);
            }
        }
    }

    private static void sanitizeList(final ListTag list) {
        if (list.getElementType() == Tag.TAG_COMPOUND) {
            for (int index = list.size() - 1; index >= 0; index--) {
                final CompoundTag entry = list.getCompound(index);
                if (isSerializedDisabledStack(entry)) {
                    list.remove(index);
                } else {
                    sanitizeCompound(entry);
                }
            }
        } else if (list.getElementType() == Tag.TAG_LIST) {
            for (int index = 0; index < list.size(); index++) {
                sanitizeList((ListTag) list.get(index));
            }
        }
    }

    private static boolean isSerializedDisabledStack(final CompoundTag compound) {
        if (!compound.contains("id", Tag.TAG_STRING) || !compound.contains("Count", Tag.TAG_ANY_NUMERIC)) {
            return false;
        }
        return isDisabledPotionDelivery(ItemStack.of(compound));
    }
}
