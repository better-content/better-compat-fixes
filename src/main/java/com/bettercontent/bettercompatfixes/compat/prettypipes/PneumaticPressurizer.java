package com.bettercontent.bettercompatfixes.compat.prettypipes;

import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;

/** Narrow interface added only to the native Pretty Pipes pressurizer. */
public interface PneumaticPressurizer {
    IAirHandlerMachine bc$getAirHandler();
}
