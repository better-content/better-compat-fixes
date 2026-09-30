package com.bettercontent.bettercompatfixes.compat.tacz;

import com.bettercontent.bettercompatfixes.mixin.tacz.ClientAssetsManagerAccessor;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.manager.DisplayManager;
import com.tacz.guns.client.resource.pojo.display.IDisplay;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoDisplay;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tacz.guns.client.resource.pojo.display.block.BlockDisplay;
import com.tacz.guns.client.resource.pojo.display.gun.GunDisplay;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.index.CommonBlockIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;

/** Repairs client display caches for index entries received from a server. */
public final class TaczDisplayCacheRecovery {
    private TaczDisplayCacheRecovery() {
    }

    public static void recover(final ResourceManager resourceManager) {
        final ClientAssetsManagerAccessor assets = (ClientAssetsManagerAccessor) (Object) ClientAssetsManager.INSTANCE;
        recoverGuns(resourceManager, assets.betterContentFixes$getGunDisplay());
        recoverAmmo(resourceManager, assets.betterContentFixes$getAmmoDisplay());
        recoverAttachments(resourceManager, assets.betterContentFixes$getAttachmentDisplay());
        recoverBlocks(resourceManager, assets.betterContentFixes$getBlockDisplay());
    }

    private static void recoverGuns(
            final ResourceManager resourceManager,
            final DisplayManager<GunDisplay> manager
    ) {
        for (Map.Entry<ResourceLocation, CommonGunIndex> entry : TimelessAPI.getAllCommonGunIndex()) {
            recover(resourceManager, manager, entry.getValue().getPojo().getDisplay(), GunDisplay.class, "display/guns");
        }
    }

    private static void recoverAmmo(
            final ResourceManager resourceManager,
            final DisplayManager<AmmoDisplay> manager
    ) {
        for (Map.Entry<ResourceLocation, CommonAmmoIndex> entry : TimelessAPI.getAllCommonAmmoIndex()) {
            recover(resourceManager, manager, entry.getValue().getPojo().getDisplay(), AmmoDisplay.class, "display/ammo");
        }
    }

    private static void recoverAttachments(
            final ResourceManager resourceManager,
            final DisplayManager<AttachmentDisplay> manager
    ) {
        for (Map.Entry<ResourceLocation, CommonAttachmentIndex> entry : TimelessAPI.getAllCommonAttachmentIndex()) {
            recover(resourceManager, manager, entry.getValue().getPojo().getDisplay(), AttachmentDisplay.class, "display/attachments");
        }
    }

    private static void recoverBlocks(
            final ResourceManager resourceManager,
            final DisplayManager<BlockDisplay> manager
    ) {
        for (Map.Entry<ResourceLocation, CommonBlockIndex> entry : TimelessAPI.getAllCommonBlockIndex()) {
            recover(resourceManager, manager, entry.getValue().getPojo().getDisplay(), BlockDisplay.class, "display/blocks");
        }
    }

    private static <T extends IDisplay> void recover(
            final ResourceManager resourceManager,
            final DisplayManager<T> manager,
            final ResourceLocation displayId,
            final Class<T> displayType,
            final String directory
    ) {
        if (displayId == null || manager.getData(displayId) != null) {
            return;
        }
        final ResourceLocation resourceId = new ResourceLocation(
                displayId.getNamespace(), directory + "/" + displayId.getPath() + ".json");
        final Optional<Resource> resource = resourceManager.getResource(resourceId);
        if (resource.isEmpty()) {
            return;
        }
        try (Reader reader = resource.get().openAsReader()) {
            final Gson gson = ClientAssetsManager.GSON;
            final JsonElement json = GsonHelper.fromJson(gson, reader, JsonElement.class, true);
            final T display = gson.fromJson(json, displayType);
            if (display != null) {
                display.init();
                manager.getAllData().put(displayId, display);
            }
        } catch (IOException | JsonParseException | IllegalArgumentException error) {
            GunMod.LOGGER.error("Failed to recover TACZ display resource {}", resourceId, error);
        }
    }
}
