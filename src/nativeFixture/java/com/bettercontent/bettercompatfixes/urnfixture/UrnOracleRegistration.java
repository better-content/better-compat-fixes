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
            // Observe the actual launched JVM, not Gradle's pre-assembly environment view.
            String root = System.getProperty("bc.urn.oracle.directory");
            String actual = System.getenv("MOD_CLASSES");
            String coordinate = BetterContentFixes.MOD_ID + "%%" + root;
            if (root == null || actual == null || !java.util.Arrays.asList(actual.split(
                    java.util.regex.Pattern.quote(java.io.File.pathSeparator))).contains(coordinate)) {
                throw new AssertionError("Generated urn oracle absent from actual JVM MOD_CLASSES");
            }
            com.mojang.logging.LogUtils.getLogger().info("BC_URN_ORACLE_MODULE_V1 nativeOnly=true coordinate={}", coordinate);
            oracle = new PinnedUrnOracle(BlockBehaviour.Properties.of().lootFrom(ModRegistry.URN));
            registration.register(new ResourceLocation(BetterContentFixes.MOD_ID, "native_urn_oracle"), oracle);
        });
    }
    public static PinnedUrnOracle original() {
        if (oracle == null) throw new AssertionError("Pinned original urn oracle was not registered");
        return oracle;
    }
}
