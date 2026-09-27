package com.bettercontent.bettercontentfixes.mixin.epicfight;

import com.bettercontent.bettercontentfixes.compat.epicfight.StyleCombat;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.skill.BasicAttack;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

@Mixin(value = BasicAttack.class, remap = false)
public abstract class BasicAttackMixin {
    @Redirect(method = "executeOnServer", at = @At(value = "INVOKE",
            target = "Lyesman/epicfight/world/capabilities/item/CapabilityItem;getAutoAttackMotion(Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;)Ljava/util/List;"),
            require = 1)
    private List<AnimationAccessor<? extends AttackAnimation>> betterContentFixes$selectedCombo(
            CapabilityItem capability, PlayerPatch<?> patch) {
        return StyleCombat.combo(capability, patch);
    }

    @Redirect(method = "setComboCounterWithEvent", at = @At(value = "INVOKE",
            target = "Lyesman/epicfight/world/capabilities/item/CapabilityItem;getAutoAttackMotion(Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;)Ljava/util/List;"),
            require = 1)
    private static List<AnimationAccessor<? extends AttackAnimation>> betterContentFixes$selectedCounterCombo(
            CapabilityItem capability, PlayerPatch<?> patch) {
        return StyleCombat.combo(capability, patch);
    }
}
