package com.bettercontent.bettercompatfixes.mixin.falloutwastelands;

import com.bettercontent.bettercompatfixes.compat.FalloutStructurePlacementBounds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps Fallout's feature-placed templates within the chunks WorldGenRegion can safely mutate. */
@Mixin(targets = "net.mcreator.falloutwastelands.world.features.StructureFeature", remap = false)
public abstract class StructureFeatureMixin {
    @Redirect(
            method = "m_142674_",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;"
                            + "m_230328_(Lnet/minecraft/world/level/ServerLevelAccessor;"
                            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;"
                            + "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;"
                            + "Lnet/minecraft/util/RandomSource;I)Z",
                    remap = false),
            remap = false,
            require = 1)
    private boolean better_compat_fixes$fitTemplateInsideWritableRegion(
            final StructureTemplate template,
            final ServerLevelAccessor level,
            final BlockPos position,
            final BlockPos pivot,
            final StructurePlaceSettings settings,
            final RandomSource random,
            final int flags
    ) {
        if (!(level instanceof WorldGenRegion region)) {
            return template.placeInWorld(level, position, pivot, settings, random, flags);
        }

        final BoundingBox bounds = template.getBoundingBox(settings, position);
        final Optional<FalloutStructurePlacementBounds.Placement> fitted =
                FalloutStructurePlacementBounds.fit(bounds, position, pivot, region.getCenter());
        if (fitted.isEmpty()) {
            return false;
        }

        final FalloutStructurePlacementBounds.Placement placement = fitted.orElseThrow();
        return template.placeInWorld(
                level, placement.position(), placement.pivot(), settings, random, flags);
    }
}
