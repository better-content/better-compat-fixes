package com.bettercontent.bettercompatfixes.mixin.malum;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Malum 1.6.7: its floating natural spirit collects into its owner without an ItemEntity pickup event. */
@Pseudo
@Mixin(targets = "com.sammy.malum.common.entity.FloatingEntity", remap = false)
public abstract class SpectatorNaturalSpiritCollectionMixin {
    private static final ResourceLocation BETTER_CONTENT$NATURAL_SPIRIT = new ResourceLocation("malum", "natural_spirit");

    @Shadow(remap = false) public LivingEntity owner;

    // The parent tick discards the spirit *immediately after* collect(). Cancel before
    // the call, not in SpiritItemEntity.collect(), or spectator-owned drops vanish.
    // Malum is distributed with an obfuscated MC tick name; accept either dev or pack spelling.
    @Inject(method = {"tick", "m_8119_"}, at = @At(value = "INVOKE", remap = false,
            target = "Lcom/sammy/malum/common/entity/FloatingEntity;collect()V"),
            cancellable = true, require = 1, expect = 1, allow = 1, remap = false)
    private void betterContentFixes$leaveSpectatorSpiritInWorld(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide && owner instanceof ServerPlayer player && player.isSpectator()
                && BETTER_CONTENT$NATURAL_SPIRIT.equals(ForgeRegistries.ENTITY_TYPES.getKey(self.getType()))) {
            ci.cancel();
        }
    }
}
