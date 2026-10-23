package com.bettercontent.bettercompatfixes.mixin.deepvoid;

import com.bettercontent.bettercompatfixes.compat.deepvoid.FleshTemplateMetadata;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Explicit field aliases because historical main hooks deliberately have no global refmap. */
@Mixin(value = StructureTemplate.class, remap = false)
public abstract class TemplateMetadataAccessor implements FleshTemplateMetadata.Template {
    @Shadow(aliases = {"f_74482_"}, remap = false) @Final
    private List<StructureTemplate.Palette> palettes;
    @Shadow(aliases = {"f_74483_"}, remap = false) @Final
    private List<StructureTemplate.StructureEntityInfo> entityInfoList;
    public List<StructureTemplate.Palette> better_compat_fixes$palettes() { return palettes; }
    public List<StructureTemplate.StructureEntityInfo> better_compat_fixes$entities() { return entityInfoList; }
}
