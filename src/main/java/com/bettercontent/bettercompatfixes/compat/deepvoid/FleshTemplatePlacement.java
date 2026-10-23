package com.bettercontent.bettercompatfixes.compat.deepvoid;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.mcreator.thedeepvoid.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import static com.bettercontent.bettercompatfixes.compat.deepvoid.FleshTemplateBounds.require;

/** Whole-template producer-coordinate repair, NOT a callback/lighting/height or RNG-history fence. */
public final class FleshTemplatePlacement {
    private FleshTemplatePlacement() {}

    public static boolean place(String structureId, StructureTemplate template, ServerLevelAccessor level,
                                BlockPos position, BlockPos pivot, StructurePlaceSettings settings,
                                RandomSource random, int flags, Operation<Boolean> original) {
        if (!(level instanceof WorldGenRegion region) || !FleshTemplateSemantics.recognizes(structureId)) {
            return original.call(template, level, position, pivot, settings, random, flags);
        }
        require(region instanceof FleshTemplateMetadata.Region, "region-accessor");
        int radius = ((FleshTemplateMetadata.Region) region).better_compat_fixes$writeRadius();
        require(radius == 1, "write-radius");
        validate(template, settings, flags);
        Vec3i size = template.getSize();
        // Detect wrap before native integer transform/bounding-box arithmetic. The local pivot is ZERO.
        int horizontal = Math.max(size.getX(), size.getZ());
        require((long) position.getX() - horizontal >= Integer.MIN_VALUE
                && (long) position.getX() + horizontal <= Integer.MAX_VALUE
                && (long) position.getZ() - horizontal >= Integer.MIN_VALUE
                && (long) position.getZ() + horizontal <= Integer.MAX_VALUE
                && (long) position.getY() + size.getY() - 1 <= Integer.MAX_VALUE, "native-bounds-overflow");
        var bounds = template.getBoundingBox(settings, position);
        boolean quarter = settings.getRotation() == Rotation.CLOCKWISE_90
                || settings.getRotation() == Rotation.COUNTERCLOCKWISE_90;
        require((long) bounds.maxX() - bounds.minX() + 1 == (quarter ? size.getZ() : size.getX())
                && (long) bounds.maxZ() - bounds.minZ() + 1 == (quarter ? size.getX() : size.getZ())
                && (long) bounds.maxY() - bounds.minY() + 1 == size.getY(), "native-bounds-shape");
        // Check every loaded palette, not merely whichever palette the original RNG later selects.
        for (var palette : ((FleshTemplateMetadata.Template) template).better_compat_fixes$palettes()) {
            for (var info : palette.blocks()) {
                require(bounds.isInside(StructureTemplate.calculateRelativePosition(settings, info.pos()).offset(position)),
                        "transformed-position");
            }
        }
        var center = region.getCenter();
        var shift = FleshTemplateBounds.fit(bounds.minX(), bounds.maxX(), bounds.minZ(), bounds.maxZ(),
                center.x, center.z, radius);
        return FleshTemplateCall.call(position, pivot, shift, FleshTemplatePlacement::translate,
                (p, q) -> original.call(template, level, p, q, settings, random, flags));
    }

    private static BlockPos translate(BlockPos position, FleshTemplateBounds.Shift shift) {
        return new BlockPos(FleshTemplateBounds.translated(position.getX(), shift.x()), position.getY(),
                FleshTemplateBounds.translated(position.getZ(), shift.z()));
    }

    private static void validate(StructureTemplate template, StructurePlaceSettings settings, int flags) {
        var processors = settings.getProcessors();
        require(processors.size() == 1 && processors.get(0) != null
                && processors.get(0).getClass() == BlockIgnoreProcessor.class, "ignore-processor");
        require(processors.get(0) instanceof FleshTemplateMetadata.Ignore, "ignore-accessor");
        var ignored = ((FleshTemplateMetadata.Ignore) processors.get(0)).better_compat_fixes$ignored();
        require(ignored != null, "ignored-block-list");
        FleshTemplateSemantics.settings(processors.size(), true, ignored.size(), ignored.contains(Blocks.AIR),
                ignored.contains(Blocks.STRUCTURE_BLOCK), BlockPos.ZERO.equals(settings.getRotationPivot()),
                settings.getBoundingBox() == null, flags);
        require(template instanceof FleshTemplateMetadata.Template, "template-accessor");
        var metadata = (FleshTemplateMetadata.Template) template;
        var palettes = metadata.better_compat_fixes$palettes();
        var entities = metadata.better_compat_fixes$entities();
        Vec3i size = template.getSize();
        require(palettes != null && entities != null && size != null, "template-metadata");
        FleshTemplateSemantics.size(size.getX(), size.getY(), size.getZ(), palettes.size(), entities.isEmpty());
        for (var palette : palettes) {
            require(palette != null && palette.blocks() != null, "palette");
            // blocks() is a bare getter. blocks(Block) would mutate the native filtered cache.
            for (var info : palette.blocks()) {
                require(info != null && info.pos() != null && info.state() != null, "block-info");
                var state = info.state();
                var block = state.getBlock();
                var pos = info.pos();
                boolean wet = state.hasProperty(BlockStateProperties.WATERLOGGED)
                        && state.getValue(BlockStateProperties.WATERLOGGED);
                FleshTemplateSemantics.block(pos.getX(), pos.getY(), pos.getZ(),
                        size.getX(), size.getY(), size.getZ(), knownBlock(block), wet, info.nbt() != null,
                        block == Blocks.STRUCTURE_BLOCK && ignored.contains(block));
            }
        }
    }

    private static boolean knownBlock(Block block) {
        if (block == Blocks.AIR || block == Blocks.STRUCTURE_BLOCK
                || block == Blocks.BONE_BLOCK || block == Blocks.SMOOTH_QUARTZ) return true;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !id.getNamespace().equals("the_deep_void")) return false;
        Class<?> expected = switch (id.getPath()) {
            case "big_teeth" -> BigTeethBlock.class;
            case "bone_marrow" -> BoneMarrowBlock.class;
            case "eye_vine" -> EyeVineBlock.class;
            case "eye_vine_stem" -> EyeVineStemBlock.class;
            case "flesh_block" -> FleshBlockBlock.class;
            case "fleshy_bone_block" -> FleshyBoneBlockBlock.class;
            case "green_eye_vine" -> GreenEyeVineBlock.class;
            case "hanging_marrow" -> HangingMarrowBlock.class;
            case "pustulent_flesh_block" -> PustulentFleshBlockBlock.class;
            case "red_eye_vine" -> RedEyeVineBlock.class;
            case "teeth_block" -> TeethBlockBlock.class;
            case "tendons" -> TendonsBlock.class;
            case "yellow_eye_vine" -> YellowEyeVineBlock.class;
            default -> null;
        };
        return expected != null && block.getClass() == expected;
    }
}
