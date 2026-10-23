package com.bettercontent.bettercompatfixes.nativefixture;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Set;

/** Getter only; tests temporarily add one existing item and restore its exact previous membership. */
@Mixin(targets = "net.mehvahdjukaar.supplementaries.reg.ModCreativeTabs", remap = false)
public interface UrnHiddenItemsAccessor {
    @Accessor(value = "HIDDEN_ITEMS", remap = false)
    static Set<Item> betterContent$hiddenItems() { throw new AssertionError("Native hidden-items accessor did not apply"); }
}
