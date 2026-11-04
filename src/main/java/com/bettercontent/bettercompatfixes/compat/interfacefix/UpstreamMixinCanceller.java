package com.bettercontent.bettercompatfixes.compat.interfacefix;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;

/**
 * Cancel only the incompatible registrations in the exact pinned third-party configs.
 * The original client Mixins remain untouched; our vibration bridge replaces VS's
 * unsupported interface injector on both physical sides. No upstream JAR is rewritten.
 *
 * MixinSquared runs this at target pre-apply, NOT while parsing a Mixin config. A
 * failure before pre-apply cannot be repaired here and must fail at startup.
 */
public final class UpstreamMixinCanceller implements MixinCanceller {
    static final String EPIC = "com.yesman.epicparcool.mixin.ParCoolMixinAnimation";
    static final String CHAIN = "org.valkyrienskies.mod.mixin.mod_compat.create.blockentity.MixinChainConveyorInteractionHandler";
    static final String VIBRATION = "org.valkyrienskies.mod.mixin.feature.sculk.MixinVibrationSystemTicker";
    static final String EPIC_TARGET = "com.alrex.parcool.common.capability.Animation";
    static final String CHAIN_TARGET = "com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorInteractionHandler";
    static final String VIBRATION_TARGET = "net.minecraft.world.level.gameevent.vibrations.VibrationSystem$Ticker";
    static final String EPIC_CONFIG_SHA = "c1980770b15805903e422f21688d41a3eef755c93832995568d7fbf036d845d7";
    static final String VS_CONFIG_SHA = "5d625e4bbcd32da1791dee967786226a217eec8b9f18e2c850547c70067d8fa3";

    static boolean decision(String mixin, List<String> targets, boolean server, String version, String configSha) {
        String expectedTarget;
        String expectedVersion;
        String expectedConfig;
        boolean cancel;
        switch (mixin) {
            case EPIC -> {
                expectedTarget = EPIC_TARGET;
                expectedVersion = "20.12.0.1";
                expectedConfig = EPIC_CONFIG_SHA;
                cancel = server; // The accessor's Animator return type is client-only.
            }
            case CHAIN -> {
                expectedTarget = CHAIN_TARGET;
                expectedVersion = "2.4.11";
                expectedConfig = VS_CONFIG_SHA;
                cancel = server; // Preserve VS's client rendering/interaction Mixin.
            }
            case VIBRATION -> {
                expectedTarget = VIBRATION_TARGET;
                expectedVersion = "2.4.11";
                expectedConfig = VS_CONFIG_SHA;
                cancel = true; // Source-owned VibrationTickerMixin replaces the injector.
            }
            default -> { return false; }
        }
        if (!targets.equals(List.of(expectedTarget)) || !expectedVersion.equals(version) || !expectedConfig.equals(configSha))
            throw new IllegalStateException("Unknown upstream Mixin target/config/version for " + mixin);
        return cancel;
    }

    private static String digest(Path path) {
        try (var input = Files.newInputStream(path)) {
            byte[] data = input.readNBytes(65537);
            if (data.length > 65536) throw new IllegalStateException("Oversize upstream Mixin config: " + path);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (IOException | NoSuchAlgorithmException error) {
            throw new IllegalStateException("Cannot verify upstream Mixin config: " + path, error);
        }
    }

    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        if (!mixinClassName.equals(EPIC) && !mixinClassName.equals(CHAIN) && !mixinClassName.equals(VIBRATION))
            return false;
        var loading = FMLLoader.getLoadingModList();
        if (loading == null) throw new IllegalStateException("Upstream Mixin cancellation has no Forge mod metadata");
        boolean epic = mixinClassName.equals(EPIC);
        String modId = epic ? "epicparcool" : "valkyrienskies";
        var file = loading.getModFileById(modId);
        if (file == null) throw new IllegalStateException("Registered upstream Mixin has no matching Forge mod: " + modId);
        var mod = file.getMods().stream().filter(m -> m.getModId().equals(modId)).findFirst().orElseThrow();
        return decision(mixinClassName, targetClassNames, FMLEnvironment.dist == Dist.DEDICATED_SERVER,
                mod.getVersion().toString(), digest(file.getFile().findResource(epic
                    ? "mixins.epicparcool.json" : "valkyrienskies-common.mixins.json")));
    }
}
