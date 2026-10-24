package com.bettercontent.bettercompatfixes.mixin.startupabi;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Exact optional-target anchor; the required plugin performs the physical-server ABI correction. */
@Pseudo
@Mixin(targets = "yesman.epicfight.world.capabilities.EpicFightCapabilities", remap = false)
public abstract class EpicFightCapabilitiesServerAbiMixin {}
