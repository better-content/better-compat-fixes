package com.bettercontent.bettercompatfixes.mixin.prettypipes;

import com.bettercontent.bettercompatfixes.compat.prettypipes.PneumaticPressurizer;
import com.bettercontent.bettercompatfixes.compat.prettypipes.PressurizerAirPolicy;
import com.bettercontent.bettercompatfixes.compat.prettypipes.PressurizerMenuAir;
import de.ellpeck.prettypipes.pressurizer.PressurizerBlockEntity;
import de.ellpeck.prettypipes.pressurizer.PressurizerContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.bettercontent.bettercompatfixes.compat.prettypipes.PressurizerAirDataSlot;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PressurizerContainer.class, remap = false)
public abstract class PressurizerContainerMixin extends AbstractContainerMenu implements PressurizerMenuAir {
    @Shadow @Final public PressurizerBlockEntity tile;
    @Unique private int bc$lowAir;
    @Unique private int bc$highAir;

    protected PressurizerContainerMixin(MenuType<?> type, int id) { super(type, id); }

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void bc$syncAir(MenuType<?> type, int id, Player player, BlockPos pos, CallbackInfo ci) {
        addDataSlot(new PressurizerAirDataSlot(() -> PressurizerAirPolicy.low(bc$serverAir()), value -> bc$lowAir = value & 0xffff));
        addDataSlot(new PressurizerAirDataSlot(() -> PressurizerAirPolicy.high(bc$serverAir()), value -> bc$highAir = value & 0xffff));
    }

    @Unique private int bc$serverAir() {
        return ((PneumaticPressurizer) tile).bc$getAirHandler().getAir();
    }

    @Override public int bc$getMenuAir() {
        return tile.getLevel() != null && !tile.getLevel().isClientSide
                ? bc$serverAir() : PressurizerAirPolicy.combine(bc$lowAir, bc$highAir);
    }
}
