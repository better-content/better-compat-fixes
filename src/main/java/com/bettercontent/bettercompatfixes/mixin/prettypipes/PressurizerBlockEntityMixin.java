package com.bettercontent.bettercompatfixes.mixin.prettypipes;

import com.bettercontent.bettercompatfixes.compat.prettypipes.PneumaticPressurizer;
import com.bettercontent.bettercompatfixes.compat.prettypipes.PressurizerAirPolicy;
import de.ellpeck.prettypipes.pressurizer.PressurizerBlockEntity;
import de.ellpeck.prettypipes.pipe.IPipeConnectable;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(value = PressurizerBlockEntity.class, remap = false)
public abstract class PressurizerBlockEntityMixin extends BlockEntity implements PneumaticPressurizer {
    @Unique private static final String BC_AIR_NBT = "better_compat_fixes:pressurizer_air";
    @Unique private IAirHandlerMachine bc$air;
    @Unique private LazyOptional<IAirHandlerMachine> bc$airCap;
    @Unique private CompoundTag bc$lastSavedAir;
    @Shadow @Final @Mutable private LazyOptional<IPipeConnectable> lazyThis;

    protected PressurizerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override public IAirHandlerMachine bc$getAirHandler() {
        if (bc$air == null) {
            bc$air = PneumaticRegistry.getInstance().getAirHandlerMachineFactory()
                    .createTierOneAirHandler(PressurizerAirPolicy.VOLUME);
            bc$airCap = LazyOptional.of(() -> bc$air);
        }
        return bc$air;
    }

    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true, require = 1)
    private <T> void bc$capability(Capability<T> cap, Direction side, CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (cap == ForgeCapabilities.ENERGY) {
            cir.setReturnValue(LazyOptional.empty());
        } else if (cap == PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY
                || cap == PNCCapabilities.AIR_HANDLER_CAPABILITY) {
            bc$getAirHandler();
            cir.setReturnValue(isRemoved() ? LazyOptional.empty() : bc$airCap.cast());
        }
    }

    @Inject(method = "pressurizeItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void bc$pressurize(ItemStack stack, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        // Forge server owns both the native routing call and payment. Never mutate on the client.
        if (level == null || level.isClientSide || isRemoved()) {
            cir.setReturnValue(false);
            return;
        }
        IAirHandlerMachine air = bc$getAirHandler();
        synchronized (air) {
            boolean canPay = PressurizerAirPolicy.canPay(air.getAir(), air.getVolume(), stack.getCount());
            if (canPay && !simulate) {
                air.addAir(-PressurizerAirPolicy.cost(stack.getCount()));
                setChanged();
            }
            cir.setReturnValue(canPay);
        }
    }

    @Inject(method = {"saveAdditional", "m_183515_"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void bc$save(CompoundTag tag, CallbackInfo ci) {
        super.saveAdditional(tag);
        tag.remove("energy");
        tag.put(BC_AIR_NBT, bc$getAirHandler().serializeNBT());
        ci.cancel();
    }

    @Inject(method = {"load", "m_142466_"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void bc$load(CompoundTag tag, CallbackInfo ci) {
        super.load(tag);
        // Missing pneumatic compound means empty, including legacy electrically charged machines.
        bc$getAirHandler().deserializeNBT(tag.getCompound(BC_AIR_NBT));
        bc$lastSavedAir = bc$air.serializeNBT();
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bc$tick(Level level, BlockPos pos, BlockState state,
                                PressurizerBlockEntity tile, CallbackInfo ci) {
        if (!level.isClientSide && !tile.isRemoved()) {
            PressurizerBlockEntityMixin self = (PressurizerBlockEntityMixin) (Object) tile;
            IAirHandlerMachine air = self.bc$getAirHandler();
            // PNCR's neighbor query uses getBlockEntity. Restrict faces to resident chunks first.
            air.setConnectedFaces(Arrays.stream(Direction.values())
                    .filter(dir -> ((ServerLevel) level).getChunkSource().getChunkNow(
                            pos.relative(dir).getX() >> 4, pos.relative(dir).getZ() >> 4) != null).toList());
            air.tick(tile);
            CompoundTag current = air.serializeNBT();
            if (!current.equals(self.bc$lastSavedAir)) {
                tile.setChanged();
                self.bc$lastSavedAir = current;
            }
            if (tile.isRemoved() || level.getBlockEntity(pos) != tile) ci.cancel();
        }
        // Native pipe association remains untouched; legacy FE storage stays permanently empty.
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        if (bc$airCap != null) bc$airCap.invalidate();
    }

    @Override public void reviveCaps() {
        super.reviveCaps();
        bc$getAirHandler();
        bc$airCap = LazyOptional.of(() -> bc$air);
        lazyThis = LazyOptional.of(() -> (IPipeConnectable) this);
    }
}
