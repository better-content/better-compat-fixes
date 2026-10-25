package com.bettercontent.bettercompatfixes.mixin.deepvoid;

import com.bettercontent.bettercompatfixes.compat.deepvoid.FleshTemplateMetadata;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Named SDK bridge; the plugin selects the separate exact-name SRG bridge in production. */
@Mixin(value = StructureTemplate.class, remap = false)
public abstract class TemplateMetadataAccessor implements FleshTemplateMetadata.Template {
    @Shadow(remap = false) @Final
    private List<StructureTemplate.Palette> palettes;
    @Shadow(remap = false) @Final
    private List<StructureTemplate.StructureEntityInfo> entityInfoList;
    public List<StructureTemplate.Palette> better_compat_fixes$palettes() { return palettes; }
    public List<StructureTemplate.StructureEntityInfo> better_compat_fixes$entities() { return entityInfoList; }
}
