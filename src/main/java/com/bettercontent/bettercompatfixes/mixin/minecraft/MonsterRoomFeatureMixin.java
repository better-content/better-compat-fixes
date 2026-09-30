package com.bettercontent.bettercompatfixes.mixin.minecraft;

import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.MonsterRoomFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reports a failed dungeon when its guarded spawner placement was rejected. */
@Mixin(MonsterRoomFeature.class)
public abstract class MonsterRoomFeatureMixin {
    /**
     * Vanilla logs an error and reports success when its guarded spawner write was rejected, such
     * as a dungeon origin on the protected bedrock floor. Preserve the protected block and report
     * the feature failure accurately instead of treating the expected missing entity as corruption.
     */
    @Inject(
            method = {"place", "m_142674_"},
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/slf4j/Logger;error(Ljava/lang/String;[Ljava/lang/Object;)V"),
            cancellable = true,
            require = 1)
    private void betterContentFixes$rejectDungeonWithoutSpawner(
            final FeaturePlaceContext<NoneFeatureConfiguration> context,
            final CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }
}
