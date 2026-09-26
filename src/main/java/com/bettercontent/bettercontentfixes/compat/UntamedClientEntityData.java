package com.bettercontent.bettercontentfixes.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;

/** Supplies Untamed Wilds' bundled entity definitions when its server-only loader is absent on a client. */
public final class UntamedClientEntityData {
    private UntamedClientEntityData() {
    }

    public static Object load(final EntityType<?> type) {
        if (FMLEnvironment.dist != Dist.CLIENT) return null;
        final ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null || !"untamedwilds".equals(id.getNamespace())) return null;
        try {
            final Class<?> holderClass = Class.forName("untamedwilds.util.EntityDataHolder");
            final String resource = "/data/untamedwilds/entities/" + id.getPath() + ".json";
            try (InputStream input = holderClass.getResourceAsStream(resource)) {
                if (input == null) return null;
                final JsonElement json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                final Codec<?> codec = (Codec<?>) holderClass.getField("CODEC").get(null);
                final Object decoded = codec.parse(JsonOps.INSTANCE, json).result()
                        .orElseThrow(() -> new IllegalStateException("Cannot decode Untamed Wilds entity data " + resource));
                final Class<?> mobClass = Class.forName("untamedwilds.entity.ComplexMob");
                final Field cacheField = mobClass.getField("ENTITY_DATA_HASH");
                @SuppressWarnings("unchecked")
                final ConcurrentHashMap<EntityType<?>, Object> cache =
                        (ConcurrentHashMap<EntityType<?>, Object>) cacheField.get(null);
                final Object existing = cache.putIfAbsent(type, decoded);
                return existing == null ? decoded : existing;
            }
        } catch (ReflectiveOperationException | java.io.IOException error) {
            throw new IllegalStateException("Cannot load Untamed Wilds entity data for " + id, error);
        }
    }
}
