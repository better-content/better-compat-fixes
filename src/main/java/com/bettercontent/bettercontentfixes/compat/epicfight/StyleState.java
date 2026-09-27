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
    private static final String KNOWN_ENTRIES = "better_content_fixes.epicfight_known_moveset_entries";
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
        if (id.isEmpty() || !learned(player, patch).contains(id)) return "";
        if (StyleCatalogue.get(patch, id) == null) restoreEntries(player, patch);
        return StyleCatalogue.get(patch, id) == null ? "" : id;
    }

    public static boolean learn(ServerPlayer player, ServerPlayerPatch patch, StyleCatalogue.Entry entry) {
        var known = learned(player, patch);
        saveEntry(player, entry);
        if (!known.add(entry.id())) return false;
        writeLearned(player, known);
        return true;
    }

    private static void saveEntry(ServerPlayer player, StyleCatalogue.Entry entry) {
        var data = persisted(player);
        var entries = data.getCompound(KNOWN_ENTRIES);
        entries.put(entry.id(), encodeEntry(entry));
        data.put(KNOWN_ENTRIES, entries);
        save(player, data);
    }

    static void restoreEntries(ServerPlayer player, ServerPlayerPatch patch) {
        var entries = persisted(player).getCompound(KNOWN_ENTRIES);
        for (var id : entries.getAllKeys()) {
            var entry = decodeEntry(id, entries.getCompound(id));
            if (entry != null) StyleCatalogue.restore(patch, entry);
        }
    }

    static CompoundTag encodeEntry(StyleCatalogue.Entry entry) {
        var data = new CompoundTag();
        data.putString("name", entry.name());
        var motions = new ListTag();
        for (var animation : entry.animations()) motions.add(StringTag.valueOf(animation.toString()));
        data.put("animations", motions);
        var sources = new ListTag();
        for (var source : entry.sources()) sources.add(StringTag.valueOf(source.toString()));
        data.put("sources", sources);
        return data;
    }

    static StyleCatalogue.Entry decodeEntry(String id, CompoundTag data) {
        var motions = data.getList("animations", Tag.TAG_STRING);
        if (motions.size() < 3 || motions.size() > 32) return null;
        var animations = new java.util.ArrayList<ResourceLocation>(motions.size());
        for (int index = 0; index < motions.size(); index++) {
            var key = ResourceLocation.tryParse(motions.getString(index));
            if (key == null) return null;
            animations.add(key);
        }
        if (!StyleCatalogue.idFor(animations).equals(id)) return null;
        var sources = new LinkedHashSet<ResourceLocation>();
        var storedSources = data.getList("sources", Tag.TAG_STRING);
        for (int index = 0; index < storedSources.size(); index++) {
            var source = ResourceLocation.tryParse(storedSources.getString(index));
            if (source != null) sources.add(source);
        }
        return new StyleCatalogue.Entry(id, data.getString("name"),
                java.util.List.copyOf(animations), java.util.Collections.unmodifiableSet(sources));
    }

    public static boolean select(ServerPlayer player, ServerPlayerPatch patch, String id) {
        if (id == null || id.length() > 64) return false;
        if (!id.isEmpty() && StyleCatalogue.get(patch, id) == null) restoreEntries(player, patch);
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
