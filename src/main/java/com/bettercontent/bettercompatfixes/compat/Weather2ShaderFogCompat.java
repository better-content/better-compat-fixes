package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import net.irisshaders.iris.api.v0.IrisApi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Weather2ShaderFogCompat {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicBoolean API_FAILURE_LOGGED = new AtomicBoolean();

    private Weather2ShaderFogCompat() {
    }

    public static boolean shouldDisableWeather2FogOverride() {
        return shouldDisableWeather2FogOverride(
                BcFixesConfig.weather2DisableFogOverrideWithShaders(),
                () -> IrisApi.getInstance().isShaderPackInUse());
    }

    static boolean shouldDisableWeather2FogOverride(
            final boolean compatibilityEnabled,
            final BooleanSupplier shaderPackActive) {
        if (!compatibilityEnabled) {
            return false;
        }

        try {
            return shaderPackActive.getAsBoolean();
        } catch (LinkageError | RuntimeException exception) {
            if (API_FAILURE_LOGGED.compareAndSet(false, true)) {
                LOGGER.warn("Could not query Oculus shader-pack state; retaining Weather2 fog overrides", exception);
            }
            return false;
        }
    }
}
