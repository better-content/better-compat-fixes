package com.bettercontent.bettercompatfixes.mixin.complicatedbees;

import com.accbdd.complicated_bees.block.entity.BaseBeeHousing;
import com.accbdd.complicated_bees.block.entity.ApiaryBlockEntity;
import com.accbdd.complicated_bees.item.QueenItem;
import com.bettercontent.bettercompatfixes.compat.bees.ColonyActivity;
import com.bettercontent.bettercompatfixes.compat.bees.ColonyAppearance;
import com.bettercontent.bettercompatfixes.compat.bees.DecodedColorScope;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native eligibility is unchanged. Sync only primitive cosmetic state on effective transitions. */
@Mixin(value = BaseBeeHousing.class, remap = false)
public abstract class ColonyActivityMixin extends BlockEntity implements ColonyActivity {
    @Unique private boolean betterCompat$active, betterCompat$known, betterCompat$pendingKnown, betterCompat$dirty;
    @Unique private int betterCompat$rgb, betterCompat$pendingRgb;

    protected ColonyActivityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    @Override public boolean betterCompat$colonyActive() {
        if (level != null && !level.isClientSide && (Object) this instanceof ApiaryBlockEntity housing) {
            // Original predicate and reads: no new slot/inventory/native eligibility query.
            return housing.getBeeItems().getStackInSlot(0).getItem() instanceof QueenItem
                    && housing.getLogic().isQueenSatisfied() && housing.getOutputBuffer().empty();
        }
        return betterCompat$active;
    }
    @Override public boolean betterCompat$colonyColorKnown() { return betterCompat$active && betterCompat$known; }
    @Override public int betterCompat$colonyColor() { return betterCompat$colonyColorKnown() ? betterCompat$rgb : 0; }
    @Override public void betterCompat$clearColonyColor() {
        betterCompat$dirty |= betterCompat$active || betterCompat$known;
        betterCompat$active = betterCompat$known = betterCompat$pendingKnown = false;
        betterCompat$rgb = betterCompat$pendingRgb = 0;
    }

    @Inject(method = "tickServer", at = @At("HEAD"), require = 1, allow = 1)
    private void betterCompat$beginColorTick(CallbackInfo ci) {
        betterCompat$pendingKnown = false; betterCompat$pendingRgb = 0;
    }

    /** Observe ONLY the already-existing expressed-effect decode; exactly one original forward. */
    @WrapOperation(method = "doBeeEffect", at = @At(value = "INVOKE",
            target = "Lcom/accbdd/complicated_bees/bees/GeneticHelper;getGeneValue(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Z)Ljava/lang/Object;"),
            require = 1, allow = 1)
    private Object betterCompat$observeExpressedEffect(ItemStack stack, ResourceLocation gene, boolean expressed,
                                                      Operation<Object> original) {
        boolean observe = expressed && (Object) this instanceof ApiaryBlockEntity;
        DecodedColorScope.Frame frame = observe ? DecodedColorScope.open() : null;
        boolean completed = false;
        try {
            Object result = original.call(stack, gene, expressed);
            completed = true;
            return result;
        } finally {
            if (observe) {
                DecodedColorScope.Sample sample = DecodedColorScope.close(frame);
                betterCompat$pendingKnown = completed && sample.known();
                betterCompat$pendingRgb = betterCompat$pendingKnown ? sample.rgb() : 0;
            }
        }
    }

    @Inject(method = "tickServer", at = @At("RETURN"), require = 1, allow = 1)
    private void betterCompat$syncActivity(CallbackInfo ci) {
        boolean active = betterCompat$colonyActive();
        boolean known = active && betterCompat$pendingKnown;
        int rgb = known ? betterCompat$pendingRgb : 0;
        if (betterCompat$dirty || active != betterCompat$active || known != betterCompat$known || rgb != betterCompat$rgb) {
            betterCompat$active = active; betterCompat$known = known; betterCompat$rgb = rgb; betterCompat$dirty = false;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        // Published primitive snapshot only: color transitions must not cause extra inventory reads.
        boolean active = betterCompat$active;
        boolean known = active && betterCompat$known;
        new ColonyAppearance(active, known, known ? betterCompat$rgb : 0).write(tag);
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void handleUpdateTag(CompoundTag tag) {
        ColonyAppearance state = ColonyAppearance.read(tag);
        betterCompat$active = state.active(); betterCompat$known = state.known(); betterCompat$rgb = state.rgb();
        betterCompat$pendingKnown = false; betterCompat$pendingRgb = 0; betterCompat$dirty = false;
    }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        handleUpdateTag(packet.getTag() == null ? new CompoundTag() : packet.getTag());
    }
    @Override public void setRemoved() {
        betterCompat$clearColonyColor();
        super.setRemoved();
    }
}
