package com.bettercontent.bettercompatfixes.compat.moonlightplatform;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.forgespi.language.IModInfo;

/** Pack-owned, selected native Forge pair distinction; not upstream endorsement or all-module support. */
public final class NativeForgeApiPlatform {
    public record Provider(String id, String version, String instanceClass, boolean javaFml,
                           boolean nativeContainerAndOwner, byte[] archive) {}
    public static final Map<String, String> INSTANCES = Map.of(
            "fabric_api", "net.fabricmc.fabric.impl.sinytra.SinytraFabric",
            "fabric_api_base", "net.fabricmc.fabric.impl.base.event.FabricApiBaseMod",
            "fabric_events_interaction_v0", "net.fabricmc.fabric.impl.event.interaction.InteractionEventsRouter",
            "fabric_resource_loader_v0", "net.fabricmc.fabric.impl.resource.loader.ResourceLoaderImpl",
            "moonlight", "net.mehvahdjukaar.moonlight.forge.MoonlightForge");
    public static final Map<String, String> VERSIONS = Map.of(
            "fabric_api", "0.92.6+1.11.14+1.20.1", "fabric_api_base", "0.4.32+ef105b4977",
            "fabric_events_interaction_v0", "0.6.5+0f26153f77", "fabric_resource_loader_v0", "0.11.12+4b4151c077",
            "moonlight", "1.20-2.16.34");
    private NativeForgeApiPlatform() {}

    /** Original presence query ALWAYS executes first; unknown/error populations retain its result. */
    public static boolean hasUnsupportedFabricApiProvider(ModList mods, String id) {
        boolean present = mods.isLoaded(id);
        if (!present || !"fabric_api".equals(id)) return present;
        if (mods.getMods().stream().anyMatch(m -> List.of("connector", "connectormod", "sinytra_connector").contains(m.getModId()))) return true;
        Map<String, Provider> providers = new HashMap<>();
        try {
            for (String expected : INSTANCES.keySet()) {
                List<IModInfo> matches = mods.getMods().stream().filter(m -> expected.equals(m.getModId())).toList();
                if (matches.size() != 1) return true;
                IModInfo info = matches.get(0);
                var container = mods.getModContainerById(expected).orElse(null);
                if (container == null || info.getOwningFile() == null || info.getVersion() == null) return true;
                Object instance = container.getMod();
                var owner = info.getOwningFile();
                if (instance == null || owner.getMods() == null || owner.getConfig() == null
                        || owner.getFile() == null || owner.getFile().getFilePath() == null) return true;
                var instanceModule = instance.getClass().getModule();
                boolean exactOwner = container.getModInfo() == info && owner.getMods().stream().filter(m -> m == info).count() == 1
                        && container.getClass().getName().equals("net.minecraftforge.fml.javafmlmod.FMLModContainer")
                        && instanceModule.isNamed() && instanceModule.getName().equals(owner.moduleName());
                boolean javaFml = owner.getConfig().getConfigElement("modLoader").filter("javafml"::equals).isPresent();
                providers.put(expected, new Provider(expected, info.getVersion().toString(), instance.getClass().getName(),
                        javaFml, exactOwner, NativeForgeApiIdentity.read(owner.getFile().getFilePath())));
            }
            String forge = version(mods, "forge"), minecraft = version(mods, "minecraft");
            return !knownNativePair(FMLEnvironment.production, forge, minecraft, providers);
        } catch (IOException unreadable) {
            // Read failure is UNKNOWN, never acceptance; original Moonlight error branch stays live.
            return true;
        }
    }
    private static String version(ModList mods, String id) {
        var matches = mods.getMods().stream().filter(m -> id.equals(m.getModId())).toList();
        return matches.size() == 1 && matches.get(0).getVersion() != null ? matches.get(0).getVersion().toString() : "";
    }
    /** Pure rejection matrix adapter, not an alternative runtime source of provider identity. */
    public static boolean knownNativePair(boolean production, String forge, String minecraft, Map<String, Provider> providers) throws IOException {
        if (!providers.keySet().equals(INSTANCES.keySet()) || !"1.20.1".equals(minecraft)) return false;
        for (String id : INSTANCES.keySet()) {
            Provider provider = providers.get(id);
            if (provider == null || !id.equals(provider.id()) || !VERSIONS.get(id).equals(provider.version())
                    || !INSTANCES.get(id).equals(provider.instanceClass()) || !provider.javaFml() || !provider.nativeContainerAndOwner()
                    || provider.archive() == null) return false;
        }
        var root = providers.get("fabric_api").archive();
        var moonlight = providers.get("moonlight").archive();
        var pair = NativeForgeApiIdentity.pair(production, root, moonlight);
        if (pair == null || !pair.forge().equals(forge)) return false;
        var entries = NativeForgeApiIdentity.entries(root);
        var moonEntries = NativeForgeApiIdentity.entries(moonlight);
        byte[] moonClass = moonEntries.get(NativeForgeApiIdentity.MOONLIGHT_CLASS);
        if (moonClass == null || !pair.moonlightClass().equals(NativeForgeApiIdentity.hash(moonClass))) return false;
        for (var module : NativeForgeApiIdentity.MODULE_PATHS.entrySet()) {
            byte[] declared = entries.get(module.getValue());
            byte[] actual = providers.get(module.getKey()).archive();
            if (declared == null || !pair.modules().get(module.getKey()).equals(NativeForgeApiIdentity.hash(declared))
                    || !java.util.Arrays.equals(declared, actual)) return false;
        }
        return true;
    }
}
