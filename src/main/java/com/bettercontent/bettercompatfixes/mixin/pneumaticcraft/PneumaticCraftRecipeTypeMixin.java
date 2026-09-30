package com.bettercontent.bettercompatfixes.mixin.pneumaticcraft;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "me.desht.pneumaticcraft.common.recipes.PneumaticCraftRecipeType", remap = false)
public abstract class PneumaticCraftRecipeTypeMixin {
    @ModifyVariable(method = "getRecipes", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private Level betterContentFixes$useActiveClientLevel(@Nullable final Level level) {
        return level == null ? Minecraft.getInstance().level : level;
    }
}
