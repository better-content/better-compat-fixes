package com.bettercontent.bettercompatfixes.mixin.epicfight;

import com.bettercontent.bettercompatfixes.compat.epicfight.StyleCombat;
import java.util.List;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = AttackAnimation.Phase.class, remap = false)
public abstract class AttackPhaseMixin {
    @Redirect(method = "getCollidingEntities", at = @At(value = "INVOKE",
            target = "Lyesman/epicfight/api/collider/Collider;updateAndSelectCollideEntity(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lyesman/epicfight/api/animation/types/AttackAnimation;FFLyesman/epicfight/api/animation/Joint;F)Ljava/util/List;"),
            require = 1)
    private List<Entity> betterContentFixes$heldWeaponCollider(Collider authored,
            LivingEntityPatch<?> patch, AttackAnimation animation, float prev, float current,
            Joint joint, float partial) {
        return StyleCombat.collide(authored, patch, animation, prev, current, joint, partial);
    }
}
