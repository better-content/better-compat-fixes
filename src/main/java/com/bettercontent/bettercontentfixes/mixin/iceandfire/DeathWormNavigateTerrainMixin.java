package com.bettercontent.bettercontentfixes.mixin.iceandfire;

import com.bettercontent.bettercontentfixes.compat.DeathWormTerrain;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;

/** Ice and Fire tests the sand tag directly in this part of death worm movement. */
@Mixin(targets = "com.github.alexthe666.iceandfire.pathfinding.PathNavigateDeathWormSand", remap = false)
public abstract class DeathWormNavigateTerrainMixin {
    @Group(name = "wormTerrain", min = 1)
    @ModifyExpressionValue(method = {"m_183431_"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/tags/BlockTags;SAND:Lnet/minecraft/tags/TagKey;", remap = false),
        remap = false, require = 0)
    private TagKey<Block> betterContentFixes$allTerrain(final TagKey<Block> original) {
        return DeathWormTerrain.BURROWABLE;
    }

    @Group(name = "wormTerrain", min = 1)
    @ModifyExpressionValue(method = {"m_183431_"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/tags/BlockTags;f_13029_:Lnet/minecraft/tags/TagKey;", remap = false),
        remap = false, require = 0)
    private TagKey<Block> betterContentFixes$allTerrainObfuscated(final TagKey<Block> original) {
        return DeathWormTerrain.BURROWABLE;
    }
}
