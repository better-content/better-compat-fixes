package com.bettercontent.bettercompatfixes.mixin.quark;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.violetmoon.zeta.module.ZetaModule;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Exact Quark 4.0-462 registered native callback, not a replacement sort implementation. */
@Mixin(targets = "org.violetmoon.quark.base.client.handler.InventoryButtonHandler$ButtonProviderHolder", remap = false)
public interface SortProviderAccessor {
    @Accessor("keybind") KeyMapping bc$keybind();
    @Accessor("pressed") Consumer<AbstractContainerScreen<?>> bc$pressed();
    @Accessor("module") ZetaModule bc$module();
    @Accessor("enableCond") BooleanSupplier bc$enableCond();
}
