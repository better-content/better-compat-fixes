package com.bettercontent.bettercompatfixes.urnfixture;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.urnfixture.oracle.PinnedUrnOracle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Source fixture only: original reviewed class, with SELF name remapped and no method rewriting. */
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class UrnOracleRegistration {
    private static PinnedUrnOracle oracle;
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, registration -> {
            oracle = new PinnedUrnOracle(BlockBehaviour.Properties.of().lootFrom(ModRegistry.URN));
            registration.register(new ResourceLocation(BetterContentFixes.MOD_ID, "native_urn_oracle"), oracle);
        });
    }
    public static PinnedUrnOracle original() {
        if (oracle == null) throw new AssertionError("Pinned original urn oracle was not registered");
        return oracle;
    }
}
