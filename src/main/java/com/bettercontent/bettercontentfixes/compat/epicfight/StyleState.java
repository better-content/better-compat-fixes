package com.bettercontent.bettercontentfixes.compat.epicfight;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/** Server-owned unlocks and one character-wide selection. */
public final class StyleState {
    private static final String LEARNED = "better_content_fixes.epicfight_learned_movesets";
    private static final String SELECTED = "better_content_fixes.epicfight_selected_moveset";
    private static final String LEGACY = "better_content_fixes.epicfight_style_discoveries";
    private static final String MIGRATED = "better_content_fixes.epicfight_movesets_migrated";

    private StyleState() {}

    private static CompoundTag persisted(ServerPlayer player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static void save(ServerPlayer player, CompoundTag data) {
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
    }

    public static Set<String> learned(ServerPlayer player, ServerPlayerPatch patch) {
        migrate(player, patch);
        var list = persisted(player).getList(LEARNED, Tag.TAG_STRING);
        var result = new LinkedHashSet<String>();
        for (int i = 0; i < list.size(); i++) result.add(list.getString(i));
        return result;
    }

    public static String selected(ServerPlayer player, ServerPlayerPatch patch) {
        migrate(player, patch);
        String id = persisted(player).getString(SELECTED);
        return id.isEmpty() || !learned(player, patch).contains(id)
                || StyleCatalogue.get(patch, id) == null ? "" : id;
    }

    public static boolean learn(ServerPlayer player, ServerPlayerPatch patch, String id) {
        var known = learned(player, patch);
        if (!known.add(id)) return false;
        writeLearned(player, known);
        return true;
    }

    public static boolean select(ServerPlayer player, ServerPlayerPatch patch, String id) {
        if (id == null || id.length() > 64) return false;
        if (!id.isEmpty() && (!learned(player, patch).contains(id)
                || StyleCatalogue.get(patch, id) == null)) return false;
        var data = persisted(player);
        data.putString(SELECTED, id);
        save(player, data);
        return true;
    }

    private static void writeLearned(ServerPlayer player, Set<String> known) {
        var data = persisted(player);
        var list = new ListTag();
        for (var id : known) list.add(StringTag.valueOf(id));
        data.put(LEARNED, list);
        save(player, data);
    }

    private static void migrate(ServerPlayer player, ServerPlayerPatch patch) {
        var data = persisted(player);
        if (data.getBoolean(MIGRATED)) return;
        var known = new LinkedHashSet<String>();
        var oldNew = data.getList(LEARNED, Tag.TAG_STRING);
        for (int i = 0; i < oldNew.size(); i++) known.add(oldNew.getString(i));
        var legacy = player.getPersistentData().getList(LEGACY, Tag.TAG_STRING);
        for (int i = 0; i < legacy.size(); i++) {
            String raw = legacy.getString(i);
            int separator = raw.indexOf('|');
            if (separator <= 0) continue;
            ResourceLocation itemId = ResourceLocation.tryParse(raw.substring(0, separator));
            if (itemId == null) continue;
            var item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) continue;
            var stack = new ItemStack(item);
            var capability = EpicFightCapabilities.getItemStackCapability(stack);
            if (capability == null) continue;
            var entry = StyleCatalogue.observe(patch, stack, capability);
            if (entry != null) known.add(entry.id());
        }
        var list = new ListTag();
        for (var id : known) list.add(StringTag.valueOf(id));
        data.put(LEARNED, list);
        data.putBoolean(MIGRATED, true);
        save(player, data);
    }
}
