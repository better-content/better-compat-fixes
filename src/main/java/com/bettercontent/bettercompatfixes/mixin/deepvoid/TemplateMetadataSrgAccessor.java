package com.bettercontent.bettercompatfixes.mixin.deepvoid;

import com.bettercontent.bettercompatfixes.compat.deepvoid.FleshTemplateMetadata;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Exact production field names; no aliases or global historical-hook refmap. */
@Mixin(value = StructureTemplate.class, remap = false)
public abstract class TemplateMetadataSrgAccessor implements FleshTemplateMetadata.Template {
    @Shadow(remap = false) @Final
    private List<StructureTemplate.Palette> f_74482_;
    @Shadow(remap = false) @Final
    private List<StructureTemplate.StructureEntityInfo> f_74483_;
    public List<StructureTemplate.Palette> better_compat_fixes$palettes() { return f_74482_; }
    public List<StructureTemplate.StructureEntityInfo> better_compat_fixes$entities() { return f_74483_; }
}
