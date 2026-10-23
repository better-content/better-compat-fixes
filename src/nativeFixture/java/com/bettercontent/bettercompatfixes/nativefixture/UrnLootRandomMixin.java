package com.bettercontent.bettercompatfixes.nativefixture;

import com.bettercontent.bettercompatfixes.urnfixture.UrnRandomScope;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Official-mapped source fixture only; the actual constructor ABI is independently checked. */
@Mixin(value = LootContext.Builder.class, remap = false)
public abstract class UrnLootRandomMixin {
    @ModifyArg(method = "create", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/storage/loot/LootContext;<init>(Lnet/minecraft/world/level/storage/loot/LootParams;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/storage/loot/LootDataResolver;Lnet/minecraft/resources/ResourceLocation;)V"),
            index = 1, require = 1, expect = 1, allow = 1, remap = false)
    private RandomSource betterContent$seedOriginalLoot(net.minecraft.world.level.storage.loot.LootParams params,
            RandomSource original, net.minecraft.world.level.storage.loot.LootDataResolver resolver,
            net.minecraft.resources.ResourceLocation table) {
        return UrnRandomScope.forLoot(original, params.getLuck());
    }
}
