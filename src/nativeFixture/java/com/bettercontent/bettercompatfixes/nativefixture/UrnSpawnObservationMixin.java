package com.bettercontent.bettercompatfixes.nativefixture;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Both the unchanged original oracle and native implementation receive identical scoped RNG input. */
@Mixin(targets = {"net.mehvahdjukaar.supplementaries.common.block.blocks.UrnBlock",
        "com.bettercontent.bettercompatfixes.nativefixture.oracle.PinnedUrnOracle"}, remap = false)
public abstract class UrnSpawnObservationMixin {
    @ModifyExpressionValue(method = "spawnAfterBreak", at = @At(value = "FIELD",
            target = "Lnet/minecraft/server/level/ServerLevel;random:Lnet/minecraft/util/RandomSource;"), require = 1, expect = 1, allow = 1)
    private RandomSource betterContent$seedOriginalChance(RandomSource original) { return UrnRandomScope.forSpawn(original); }
    @ModifyExpressionValue(method = "spawnAfterBreak", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getRandom()Lnet/minecraft/util/RandomSource;"), require = 1, expect = 1, allow = 1)
    private RandomSource betterContent$seedOriginalSelection(RandomSource original) { return UrnRandomScope.forSpawn(original); }
    @WrapOperation(method = "spawnAfterBreak", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"), require = 1, expect = 1, allow = 1)
    private boolean betterContent$observeActualInsertion(ServerLevel level, Entity entity, Operation<Boolean> original) {
        boolean inserted = original.call(level, entity);
        UrnRandomScope.observeSpawn(entity, inserted);
        return inserted;
    }
}
