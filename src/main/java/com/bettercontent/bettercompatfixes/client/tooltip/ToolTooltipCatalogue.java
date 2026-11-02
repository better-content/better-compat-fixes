package com.bettercontent.bettercompatfixes.client.tooltip;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Immutable resource metadata only; never caches an item, player, definition or computed tooltip. */
public final class ToolTooltipCatalogue {
    private static final Logger LOG = LogUtils.getLogger();
    public static final ResourceLocation LOCATION = new ResourceLocation("better_compat_fixes", "tooltips/effects.json");
    private static final Gson GSON = new Gson();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    public record Fragment(String key, String text, Set<String> requires, String kind, List<Double> per_level,
                           boolean named) {
        public Fragment {
            requires = requires == null ? Set.of() : Set.copyOf(requires);
            kind = kind == null ? "behavior" : kind;
            per_level = per_level == null ? List.of() : List.copyOf(per_level);
            if (key == null || text == null) throw new IllegalArgumentException("Effect key/text required");
            ToolTooltipEffect.Category.valueOf(kind.toUpperCase(java.util.Locale.ROOT));
        }
    }
    public record Data(Map<String, List<Fragment>> modifiers, Map<String, List<Fragment>> actions,
                       Map<String, List<String>> moduleTypes) {
        public Data {
            modifiers = immutable(modifiers); actions = immutable(actions);
            moduleTypes = immutable(moduleTypes == null ? Map.of() : moduleTypes);
        }
        private static <T> Map<String, List<T>> immutable(Map<String, List<T>> map) {
            var result = new java.util.LinkedHashMap<String, List<T>>();
            map.forEach((key, value) -> result.put(key, List.copyOf(value)));
            return Map.copyOf(result);
        }
    }
    private static volatile Data current = builtin();
    private ToolTooltipCatalogue() {}
    public static Data current() { return current; }
    public static Data parse(JsonObject json) {
        if (json.get("schema").getAsInt() != 1) throw new IllegalArgumentException("Unsupported tooltip schema");
        return GSON.fromJson(json, Data.class);
    }
    private static Data builtin() {
        try (var input = ToolTooltipCatalogue.class.getResourceAsStream("/assets/better_compat_fixes/tooltips/effects.json")) {
            if (input == null) throw new IllegalStateException("Missing builtin tooltip descriptors");
            return parse(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    public static void reload(ResourceManager manager) {
        try {
            var resource = manager.getResource(LOCATION);
            if (resource.isEmpty()) current = builtin();
            else try (var reader = resource.get().openAsReader()) {
                current = parse(JsonParser.parseReader(reader).getAsJsonObject());
            }
            WARNED.clear();
        } catch (RuntimeException | java.io.IOException e) {
            LOG.error("Invalid tool tooltip resource; retaining previous valid descriptors", e);
        }
    }
    public static void warnOnce(String key, Throwable error) {
        if (WARNED.size() < 512 && WARNED.add(key)) {
            if (error == null) LOG.warn("Tool tooltip descriptor coverage missing: {}", key);
            else LOG.warn("Tool tooltip unavailable for {}", key, error);
        }
    }
}
