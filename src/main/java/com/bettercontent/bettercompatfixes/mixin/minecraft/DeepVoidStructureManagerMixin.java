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
    @Shadow(aliases = {"f_220460_"}, remap = false) @Final private LevelAccessor level;
    @Override public boolean better_compat_fixes$isOverworld() {
        return level instanceof ServerLevelAccessor server && server.getLevel().dimension().equals(Level.OVERWORLD);
    }
}
