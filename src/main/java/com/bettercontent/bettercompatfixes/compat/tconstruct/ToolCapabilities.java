package com.bettercontent.bettercompatfixes.compat.tconstruct;

import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;

/** Definition capability, not current effectiveness against a block. Never cache across reloads. */
public final class ToolCapabilities {
    private ToolCapabilities() {}

    public static boolean hasMiningDefinition(ToolDefinition definition) {
        return definition != null && definition.isDataLoaded()
                && definition.getData().getHooks().hasHook(ToolHooks.IS_EFFECTIVE);
    }
}
