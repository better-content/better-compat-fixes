package com.bettercontent.bettercompatfixes.mixin.thirst;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ghen.thirst.foundation.common.loot.AddLootTableModifier", remap = false)
public abstract class AddLootTableModifierMixin {
    @Shadow(remap = false)
    @Final
    private ResourceLocation lootTable;

    @Inject(method = "doApply", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void betterContentFixes$generateNestedLootWithoutGlobalModifiers(
            final ObjectArrayList<ItemStack> generatedLoot,
            final LootContext context,
            final CallbackInfoReturnable<ObjectArrayList<ItemStack>> callback
    ) {
        final LootTable nestedTable = context.getResolver().getLootTable(lootTable);
        nestedTable.getRandomItemsRaw(
                context,
                LootTable.createStackSplitter(context.getLevel(), generatedLoot::add));
        callback.setReturnValue(generatedLoot);
    }
}
