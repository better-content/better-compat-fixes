package com.bettercontent.bettercompatfixes.compat.deepvoid;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Typed views implemented by actual field-alias Mixins; ordinary interfaces, not fake SDK types. */
public interface FleshTemplateMetadata {
    interface Template {
        List<StructureTemplate.Palette> better_compat_fixes$palettes();
        List<StructureTemplate.StructureEntityInfo> better_compat_fixes$entities();
    }
    interface Ignore { ImmutableList<Block> better_compat_fixes$ignored(); }
    interface Region { int better_compat_fixes$writeRadius(); }
}
