package com.bettercontent.bettercompatfixes.mixin.sophisticatedstorage;

import com.bettercontent.bettercompatfixes.compat.SophisticatedBarrelHopperSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = StorageBlockEntity.class, remap = false)
public abstract class StorageBlockEntityMixin implements SophisticatedBarrelHopperSource {
    @Shadow
    public abstract StorageWrapper getStorageWrapper();

    @Override
    public boolean betterContent$isBarrel() {
        final ResourceLocation typeId = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(
                ((BlockEntity) (Object) this).getType());
        if (typeId == null || !"sophisticatedstorage".equals(typeId.getNamespace())) {
            return false;
        }
        return "barrel".equals(typeId.getPath()) || "limited_barrel".equals(typeId.getPath());
    }

    @Override
    public IItemHandlerModifiable betterContent$getInventoryForInputOutput() {
        return getStorageWrapper().getInventoryForInputOutput();
    }
}
