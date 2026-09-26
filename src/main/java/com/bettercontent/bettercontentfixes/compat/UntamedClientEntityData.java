package com.bettercontent.bettercontentfixes.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import untamedwilds.entity.ComplexMob;
import untamedwilds.util.EntityDataHolder;

/** Supplies Untamed Wilds' bundled entity definitions when its server-only loader is absent on a client. */
public final class UntamedClientEntityData {
    private UntamedClientEntityData() {
    }

    public static EntityDataHolder load(final EntityType<?> type) {
        if (FMLEnvironment.dist != Dist.CLIENT) return null;
        final ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null || !"untamedwilds".equals(id.getNamespace())) return null;
        final String resource = "/data/untamedwilds/entities/" + id.getPath() + ".json";
        try (InputStream input = EntityDataHolder.class.getResourceAsStream(resource)) {
            if (input == null) return null;
            final JsonElement json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            final EntityDataHolder decoded = EntityDataHolder.CODEC.parse(JsonOps.INSTANCE, json).result()
                    .orElseThrow(() -> new IllegalStateException("Cannot decode Untamed Wilds entity data " + resource));
            final EntityDataHolder existing = ComplexMob.ENTITY_DATA_HASH.putIfAbsent(type, decoded);
            return existing == null ? decoded : existing;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot load Untamed Wilds entity data for " + id, error);
        }
    }
}
