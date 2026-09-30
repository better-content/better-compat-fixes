package com.bettercontent.bettercompatfixes.mixin.lostcities;

import com.bettercontent.bettercompatfixes.compat.LostCitiesC2meDhSerialization;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "mcjty.lostcities.setup.ForgeEventHandlers", remap = false)
public abstract class ForgeEventHandlersMixin {
    @WrapMethod(method = "onServerStarting", remap = false)
    private void better_compat_fixes$resetLostCitiesStopState(
            final ServerAboutToStartEvent event,
            final Operation<Void> original) {
        LostCitiesC2meDhSerialization.beginServerStarting();
        LostCitiesC2meDhSerialization.runSerialized(() -> original.call(event));
    }

    @WrapMethod(method = "onServerStopping", remap = false)
    private void better_compat_fixes$serializeLostCitiesShutdown(
            final ServerStoppingEvent event,
            final Operation<Void> original) {
        LostCitiesC2meDhSerialization.beginServerStopping();
        original.call(event);
    }
}
