package com.bettercontent.bettercompatfixes.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

final class Weather2ShaderFogCompatTest {
    @Test
    void disablesWeather2FogOnlyWhileCompatibilityAndShadersAreActive() {
        assertTrue(Weather2ShaderFogCompat.shouldDisableWeather2FogOverride(true, () -> true));
        assertFalse(Weather2ShaderFogCompat.shouldDisableWeather2FogOverride(true, () -> false));
    }

    @Test
    void disabledCompatibilityDoesNotQueryOptionalOculusApi() {
        AtomicBoolean queried = new AtomicBoolean();

        assertFalse(Weather2ShaderFogCompat.shouldDisableWeather2FogOverride(false, () -> {
            queried.set(true);
            return true;
        }));
        assertFalse(queried.get());
    }

    @Test
    void optionalOculusApiFailureRetainsWeather2Fog() {
        assertFalse(Weather2ShaderFogCompat.shouldDisableWeather2FogOverride(true, () -> {
            throw new NoClassDefFoundError("simulated optional Oculus API drift");
        }));
    }
}
