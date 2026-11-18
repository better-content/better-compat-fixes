package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.deepvoid.DeepVoidStructureLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StructureManager.class, remap = false)
public abstract class DeepVoidStructureManagerMixin implements DeepVoidStructureLevel {
    // Retain the constructor's level without shadowing its name: Forge dev uses
    // 'level' while production uses 'f_220460_'. No registry or save data changes.
    @Unique private LevelAccessor better_compat_fixes$level;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void better_compat_fixes$captureLevel(LevelAccessor level, WorldOptions options,
            StructureCheck check, CallbackInfo callback) {
        better_compat_fixes$level = level;
    }

    @Override public boolean better_compat_fixes$isOverworld() {
        return better_compat_fixes$level instanceof ServerLevelAccessor server
                && server.getLevel().dimension().equals(Level.OVERWORLD);
    }
}
