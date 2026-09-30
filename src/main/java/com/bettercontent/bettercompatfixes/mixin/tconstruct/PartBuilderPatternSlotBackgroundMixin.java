package com.bettercontent.bettercompatfixes.mixin.tconstruct;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.BaseTabbedScreen;
import slimeknights.tconstruct.tables.client.inventory.PartBuilderScreen;
import slimeknights.tconstruct.tables.menu.PartBuilderContainerMenu;

/** Covers the pattern slot frame and empty-pattern icon baked into the Part Builder background. */
@Mixin(PartBuilderScreen.class)
public abstract class PartBuilderPatternSlotBackgroundMixin
        extends BaseTabbedScreen<PartBuilderBlockEntity, PartBuilderContainerMenu> {
    private static final int GUI_BACKGROUND_COLOR = 0xFFC6C6C6;

    protected PartBuilderPatternSlotBackgroundMixin(
            PartBuilderContainerMenu menu,
            Inventory inventory,
            Component title) {
        super(menu, inventory, title);
    }

    @Inject(
            method = {
                "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
                "m_7286_(Lnet/minecraft/client/gui/GuiGraphics;FII)V"
            },
            at = @At("RETURN"),
            require = 1,
            remap = false)
    private void better_compat_fixes$hidePatternSlotBackground(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY,
            CallbackInfo callback) {
        graphics.fill(cornerX + 7, cornerY + 42, cornerX + 25, cornerY + 60, GUI_BACKGROUND_COLOR);
    }
}
