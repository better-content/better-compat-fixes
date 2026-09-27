package com.bettercontent.bettercontentfixes.compat.epicfight;

import com.bettercontent.bettercontentfixes.mixin.epicfight.WeaponCapabilityAccessor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.TridentCapability;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

/** Loaded weapon forms, keyed by the exact ordered native basic-attack animations. */
public final class StyleCatalogue {
    private static final String TINKERS_MELEE_CAPABILITY =
            "com.minhhjjj.epicfighttinkercompat.tool.capabilities.TCWeaponCapability";
    public record Entry(String id, String name, List<ResourceLocation> animations, Set<ResourceLocation> sources) {
        Entry withSource(ResourceLocation source) {
            var merged = new LinkedHashSet<>(sources);
            merged.add(source);
            return new Entry(id, name, animations, Collections.unmodifiableSet(merged));
        }
    }

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static boolean scanned;

    private StyleCatalogue() {}

    public static synchronized void invalidate() {
        ENTRIES.clear();
        scanned = false;
    }

    public static synchronized List<Entry> entries(PlayerPatch<?> patch) {
        scan(patch);
        return List.copyOf(ENTRIES.values());
    }

    public static synchronized Entry get(PlayerPatch<?> patch, String id) {
        scan(patch);
        return ENTRIES.get(id);
    }

    public static synchronized void restore(PlayerPatch<?> patch, Entry entry) {
        scan(patch);
        var existing = ENTRIES.get(entry.id());
        if (existing == null) {
            ENTRIES.put(entry.id(), entry);
        } else {
            for (var source : entry.sources()) existing = existing.withSource(source);
            ENTRIES.put(entry.id(), existing);
        }
    }

    public static synchronized Entry observe(PlayerPatch<?> patch, ItemStack stack, CapabilityItem capability) {
        scan(patch);
        if (!melee(capability)) return null;
        var itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return null;
        return add(capability, patch, itemId);
    }

    public static boolean melee(CapabilityItem capability) {
        if (capability == null) return false;
        if (capability instanceof WeaponCapability || capability instanceof TridentCapability) return true;
        // Epic Fight: Tinkers Integration wraps melee tools in its own CapabilityItem subclass.
        // Match the concrete wrapper by name so the integration remains optional at runtime.
        return capability.getClass().getName().equals(TINKERS_MELEE_CAPABILITY);
    }

    private static void scan(PlayerPatch<?> patch) {
        if (scanned) return;
        scanned = true;
        for (var item : ForgeRegistries.ITEMS.getValues()) {
            var id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) continue;
            var stack = new ItemStack(item);
            try {
                var capability = EpicFightCapabilities.getItemStackCapability(stack);
                if (!melee(capability)) continue;
                add(capability, patch, id);
                if (capability instanceof WeaponCapability weapon) {
                    var variants = ((WeaponCapabilityAccessor) weapon).betterContentFixes$autoAttackMotions();
                    variants.forEach((style, motions) -> addMotion(capability, id, motions, style));
                }
            } catch (RuntimeException ignored) {
                // Some capabilities require an initialized tool stack; actual use is observed later.
            }
        }
    }

    private static Entry add(CapabilityItem capability, PlayerPatch<?> patch, ResourceLocation source) {
        List<AnimationAccessor<? extends AttackAnimation>> nativeMotion;
        try {
            nativeMotion = capability.getAutoAttackMotion(patch);
        } catch (RuntimeException ignored) {
            return null;
        }
        return addMotion(capability, source, nativeMotion, null);
    }

    private static Entry addMotion(CapabilityItem capability, ResourceLocation source,
            List<AnimationAccessor<? extends AttackAnimation>> motions, Style style) {
        if (motions == null || motions.size() < 3) return null;
        var animations = new ArrayList<ResourceLocation>(motions.size());
        for (var animation : motions) {
            if (animation == null || animation.registryName() == null) return null;
            animations.add(animation.registryName());
        }
        String id = idFor(animations);
        var existing = ENTRIES.get(id);
        if (existing != null) {
            var merged = existing.withSource(source);
            ENTRIES.put(id, merged);
            return merged;
        }
        WeaponCategory category = capability.getWeaponCategory();
        String name = category == null ? source.getPath().replace('_', ' ')
                : WeaponCategory.ENUM_MANAGER.toTranslated(category);
        if (style != null && style != CapabilityItem.Styles.COMMON) {
            name += " · " + style.toString().toLowerCase(Locale.ROOT).replace('_', ' ');
        }
        var created = new Entry(id, name, List.copyOf(animations), Set.of(source));
        ENTRIES.put(id, created);
        return created;
    }

    public static String idFor(List<ResourceLocation> animations) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (var animation : animations) {
                digest.update(animation.toString().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
