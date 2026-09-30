package com.bettercontent.bettercompatfixes.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

final class PotionAcquisitionPolicyTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError error) {
            final boolean expectedForgeHarnessFailure = java.util.stream.Stream.iterate(
                            (Throwable) error, java.util.Objects::nonNull, Throwable::getCause)
                    .anyMatch(cause -> cause instanceof NoSuchMethodException
                            && cause.getMessage() != null
                            && cause.getMessage().startsWith("net.minecraftforge.network.NetworkEvent"));
            if (!expectedForgeHarnessFailure) {
                throw error;
            }
        }
    }

    @Test
    void keepsOnlyPlainWaterWithinTheVanillaDrinkableFamily() {
        final ItemStack water = potion(Items.POTION, Potions.WATER);
        water.getOrCreateTag().putInt("Purity", 3);
        final ItemStack customWater = potion(Items.POTION, Potions.WATER);
        PotionUtils.setCustomEffects(customWater, List.of(new MobEffectInstance(MobEffects.POISON)));

        assertFalse(PotionAcquisitionPolicy.isDisabledPotionDelivery(water));
        assertTrue(PotionAcquisitionPolicy.isDisabledPotionDelivery(potion(Items.POTION, Potions.HEALING)));
        assertTrue(PotionAcquisitionPolicy.isDisabledPotionDelivery(potion(Items.POTION, Potions.AWKWARD)));
        assertTrue(PotionAcquisitionPolicy.isDisabledPotionDelivery(new ItemStack(Items.POTION)));
        assertTrue(PotionAcquisitionPolicy.isDisabledPotionDelivery(customWater));
        assertFalse(PotionAcquisitionPolicy.isDisabledPotionDelivery(new ItemStack(Items.HONEY_BOTTLE)));
    }

    @Test
    void disablesEverySplashLingeringAndTippedVariant() {
        assertTrue(PotionAcquisitionPolicy.isDisabledPotionDelivery(potion(Items.SPLASH_POTION, Potions.WATER)));
        assertTrue(PotionAcquisitionPolicy.isDisabledPotionDelivery(potion(Items.LINGERING_POTION, Potions.WATER)));
        assertTrue(PotionAcquisitionPolicy.isDisabledPotionDelivery(potion(Items.TIPPED_ARROW, Potions.POISON)));
    }

    @Test
    void recursivelySanitizesACopyOfStructureNbt() {
        final CompoundTag source = new CompoundTag();
        final ListTag items = new ListTag();
        items.add(serialized(potion(Items.POTION, Potions.WATER), 0));
        items.add(serialized(potion(Items.POTION, Potions.HEALING), 1));
        items.add(serialized(new ItemStack(Items.DIAMOND), 2));
        final CompoundTag nested = new CompoundTag();
        nested.put("Items", items);
        source.put("NestedInventory", nested);
        source.putString("Unrelated", "preserved");

        final CompoundTag sanitized = PotionAcquisitionPolicy.sanitizeBlockEntityNbt(source);
        final ListTag sanitizedItems = sanitized.getCompound("NestedInventory").getList("Items", CompoundTag.TAG_COMPOUND);

        assertEquals(3, source.getCompound("NestedInventory").getList("Items", CompoundTag.TAG_COMPOUND).size());
        assertEquals(2, sanitizedItems.size());
        assertEquals((byte) 0, sanitizedItems.getCompound(0).getByte("Slot"));
        assertEquals((byte) 2, sanitizedItems.getCompound(1).getByte("Slot"));
        assertEquals("preserved", sanitized.getString("Unrelated"));
    }

    private static ItemStack potion(final net.minecraft.world.item.Item item,
                                    final net.minecraft.world.item.alchemy.Potion potion) {
        return PotionUtils.setPotion(new ItemStack(item), potion);
    }

    private static CompoundTag serialized(final ItemStack stack, final int slot) {
        final CompoundTag serialized = stack.save(new CompoundTag());
        serialized.putByte("Slot", (byte) slot);
        return serialized;
    }
}
