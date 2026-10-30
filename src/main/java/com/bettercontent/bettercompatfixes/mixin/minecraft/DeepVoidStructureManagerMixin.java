package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.deepvoid.DeepVoidStructureLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = StructureManager.class, remap = false)
public abstract class DeepVoidStructureManagerMixin implements DeepVoidStructureLevel {
    // Production's StructureManager field is private f_220460_. Mixin aliases
    // are not supported for this transformed field; shadow its exact runtime name.
    @Shadow(remap = false) @Final private LevelAccessor f_220460_;
    @Override public boolean better_compat_fixes$isOverworld() {
        return f_220460_ instanceof ServerLevelAccessor server && server.getLevel().dimension().equals(Level.OVERWORLD);
    }
}
