package com.bettercontent.bettercompatfixes.mixin.dtaether;

import com.bettercontent.bettercompatfixes.compat.DtaetherTagCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TagEntry.class)
abstract class TagEntryMixin {
    @Shadow(remap = false)
    @Final
    private ResourceLocation f_215913_;

    @Shadow(remap = false)
    @Final
    @Mutable
    private boolean f_215915_;

    @Inject(
            method = "<init>(Lnet/minecraft/resources/ResourceLocation;ZZ)V",
            at = @At("RETURN"),
            require = 1
    )
    private void betterContent$makeStructuredDtaetherBranchOptional(
            final ResourceLocation id,
            final boolean tag,
            final boolean required,
            final CallbackInfo callback
    ) {
        betterContent$makeDtaetherBranchOptional();
    }

    @Inject(
            method = "<init>(Lnet/minecraft/util/ExtraCodecs$TagOrElementLocation;Z)V",
            at = @At("RETURN"),
            require = 1
    )
    private void betterContent$makeCompactDtaetherBranchOptional(
            final ExtraCodecs.TagOrElementLocation location,
            final boolean required,
            final CallbackInfo callback
    ) {
        betterContent$makeDtaetherBranchOptional();
    }

    private void betterContent$makeDtaetherBranchOptional() {
        if (DtaetherTagCompat.shouldTreatAsOptional(f_215913_)) {
            f_215915_ = false;
        }
    }
}
