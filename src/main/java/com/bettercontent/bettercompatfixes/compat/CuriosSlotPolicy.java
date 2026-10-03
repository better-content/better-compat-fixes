package com.bettercontent.bettercompatfixes.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import top.theillusivec4.curios.api.event.CurioEquipEvent;

import java.util.Map;
import java.util.Set;

/** One functional slot per approved type, with legacy item tags mapped by physical placement. */
public final class CuriosSlotPolicy {
    private static final Map<String, Set<String>> SOURCE_TAGS = Map.ofEntries(
            Map.entry("head", Set.of("head")),
            Map.entry("necklace", Set.of("necklace")),
            Map.entry("body", Set.of("body", "brooch", "living_armour_socket")),
            Map.entry("hands", Set.of("hands", "bracelet")),
            Map.entry("belt", Set.of("belt", "bundle", "waist")),
            Map.entry("feet", Set.of("feet")),
            Map.entry("ring", Set.of("ring", "rings")),
            Map.entry("charm", Set.of("charm", "rune", "talisman", "an_focus", "curio")),
            Map.entry("back", Set.of("back")),
            Map.entry("hook", Set.of("hook")),
            Map.entry("water", Set.of("water"))
    );

    private CuriosSlotPolicy() {}

    private static boolean tagged(ItemStack stack, String slot) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation("curios", slot));
        return stack.is(tag);
    }

    @SubscribeEvent
    public static void onEquip(CurioEquipEvent event) {
        if (!(event.getSlotContext().entity() instanceof Player)) return;
        final String slot = event.getSlotContext().identifier();
        final ItemStack stack = event.getStack();
        if (event.getSlotContext().index() != 0 || event.getSlotContext().cosmetic() || !valid(slot, stack)) {
            event.setResult(Event.Result.DENY);
        }
    }

    public static boolean valid(String slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof BackpackItem) return slot.equals("better_backpack");
        if (slot.equals("better_backpack")) return false;
        // Better Drinking Water defines a validator for this slot instead of an item tag.
        if (slot.equals("water")) return stack.is(Items.POTION) && PotionUtils.getPotion(stack) == Potions.WATER;
        Set<String> tags = SOURCE_TAGS.get(slot);
        if (tags == null) return false;
        // Generic Curios items occupy the charm slot only. Specific placement wins over
        // the catch-all curio tag so it cannot bypass an accessory choice.
        boolean specificallyTagged = SOURCE_TAGS.values().stream()
                .flatMap(Set::stream).filter(source -> !source.equals("curio"))
                .anyMatch(source -> tagged(stack, source));
        return tags.stream().filter(source -> !source.equals("curio"))
                .anyMatch(source -> tagged(stack, source))
                || slot.equals("charm") && !specificallyTagged && tagged(stack, "curio");
    }
}
