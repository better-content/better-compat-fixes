package com.bettercontent.bettercompatfixes.client.tooltip;

import yesman.epicfight.world.capabilities.item.CapabilityItem;

/** Runtime bridge implemented by the optional Tinker weapon capability mixin. */
public interface TinkerWeaponTooltipAccess extends InnateTooltipAccess {
    CapabilityItem bettercontent$tooltipDelegatedWeapon();
}
