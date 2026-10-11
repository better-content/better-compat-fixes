package com.bettercontent.bettercompatfixes.mixin.prettypipes;

import com.bettercontent.bettercompatfixes.compat.prettypipes.PressurizerAirPolicy;
import com.bettercontent.bettercompatfixes.compat.prettypipes.PressurizerMenuAir;
import de.ellpeck.prettypipes.pressurizer.PressurizerContainer;
import de.ellpeck.prettypipes.pressurizer.PressurizerGui;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTooltipArea;
import me.desht.pneumaticcraft.client.render.pressure_gauge.PressureGaugeRenderer2D;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

/** Client-only host for PNCR's actual machine-screen gauge and native tooltip-area widget. */
@Mixin(value = PressurizerGui.class, remap = false)
public abstract class PressurizerGuiMixin extends AbstractContainerScreen<PressurizerContainer> {
    @Unique private static final ResourceLocation BC_TEXTURE =
            new ResourceLocation("prettypipes", "textures/gui/pressurizer.png");
    @Unique private IAirHandlerMachine bc$displayAir;

    protected PressurizerGuiMixin(PressurizerContainer menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override protected void init() {
        super.init();
        // Per-screen view; no ticking/equalization and no mutation of the client world BE.
        bc$displayAir = PneumaticRegistry.getInstance().getAirHandlerMachineFactory()
                .createTierOneAirHandler(PressurizerAirPolicy.VOLUME);
        addRenderableWidget(new WidgetTooltipArea(leftPos + 125, topPos + 7, 44, 42, () -> {
            bc$updateDisplayAir();
            var lines = new ArrayList<Component>();
            bc$displayAir.printManometerMessage(minecraft.player, lines);
            return Tooltip.create(Component.empty().append(lines.get(0)));
        }));
    }

    @Unique private void bc$updateDisplayAir() {
        int value = ((PressurizerMenuAir) menu).bc$getMenuAir();
        // Native deserialization handles the signed integer without delta overflow.
        var state = new net.minecraft.nbt.CompoundTag();
        state.putInt("Air", value);
        bc$displayAir.deserializeNBT(state);
    }

    @Inject(method = {"render", "m_88315_"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void bc$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        // Do not run native Pretty Pipes' obsolete FE hover branch.
        ci.cancel();
    }

    @Inject(method = {"renderBg", "m_7286_"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void bc$background(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        graphics.blit(BC_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // Erase only the baked-in FE recess. Slots/title/menu remain native.
        graphics.fill(leftPos + 25, topPos + 21, leftPos + 151, topPos + 35, 0xffc6c6c6);
        ci.cancel();
    }

    @Inject(method = {"renderLabels", "m_280003_"}, at = @At("RETURN"), require = 1)
    private void bc$gauge(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        bc$updateDisplayAir();
        // PNCR renders directly through Tesselator; flush queued GUI background geometry first.
        graphics.flush();
        // Exact PNCR machine-screen renderer: min, critical, danger, working, current, center.
        PressureGaugeRenderer2D.drawPressureGauge(graphics, font, -1.0F,
                bc$displayAir.getCriticalPressure(), bc$displayAir.getDangerPressure(),
                PressurizerAirPolicy.MIN_WORKING_PRESSURE, bc$displayAir.getPressure(), 147, 28);
        graphics.drawString(font, Component.translatable("gui.better_compat_fixes.pressurizer.minimum"),
                8, 20, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.better_compat_fixes.pressurizer.consumption"),
                8, 31, 0x404040, false);
    }
}
