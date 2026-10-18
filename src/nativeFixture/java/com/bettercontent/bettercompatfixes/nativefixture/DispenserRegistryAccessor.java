package com.bettercontent.bettercompatfixes.nativefixture;

import java.util.Map;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Getter-only source fixture for the pinned private static final native registry (0x001a). */
@Mixin(DispenserBlock.class)
public interface DispenserRegistryAccessor {
    @Accessor(value = "DISPENSER_REGISTRY", remap = false)
    static Map<Item, DispenseItemBehavior> better_compat_fixes$registry() {
        throw new AssertionError("Native source fixture accessor was not transformed");
    }
}
