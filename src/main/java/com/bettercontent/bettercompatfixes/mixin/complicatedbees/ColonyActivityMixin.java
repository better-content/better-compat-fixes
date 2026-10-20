package com.bettercontent.bettercompatfixes.mixin.complicatedbees;

import com.accbdd.complicated_bees.block.entity.BaseBeeHousing;
import com.accbdd.complicated_bees.block.entity.ApiaryBlockEntity;
import com.accbdd.complicated_bees.item.QueenItem;
import com.bettercontent.bettercompatfixes.compat.bees.ColonyActivity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native housing does not sync activity. Send only one cosmetic bit, on transitions/chunk load. */
@Mixin(value = BaseBeeHousing.class, remap = false)
public abstract class ColonyActivityMixin extends BlockEntity implements ColonyActivity {
    @Unique private boolean betterCompat$active;
    @Unique private static final String BETTER_COMPAT_ACTIVITY = "better_compat_fixes:colony_active";

    protected ColonyActivityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean betterCompat$colonyActive() {
        if (level != null && !level.isClientSide && (Object) this instanceof ApiaryBlockEntity housing) {
            return housing.getBeeItems().getStackInSlot(0).getItem() instanceof QueenItem
                    && housing.getLogic().isQueenSatisfied() && housing.getOutputBuffer().empty();
        }
        return betterCompat$active;
    }

    @Inject(method = "tickServer", at = @At("RETURN"), require = 1)
    private void betterCompat$syncActivity(CallbackInfo ci) {
        boolean active = betterCompat$colonyActive();
        if (active != betterCompat$active) {
            betterCompat$active = active;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(BETTER_COMPAT_ACTIVITY, betterCompat$colonyActive());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        betterCompat$active = tag.getBoolean(BETTER_COMPAT_ACTIVITY);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }
}
