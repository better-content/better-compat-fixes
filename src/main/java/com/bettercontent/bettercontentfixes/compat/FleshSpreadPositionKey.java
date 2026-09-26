package com.bettercontent.bettercontentfixes.compat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;

/** Reads both TFTH's old BlockPos.toString keys and the packed keys its loader expects. */
public final class FleshSpreadPositionKey {
    private static final Pattern PRINTED_POSITION = Pattern.compile(
            "BlockPos\\{x=(-?[0-9]+), y=(-?[0-9]+), z=(-?[0-9]+)\\}");

    private FleshSpreadPositionKey() {
    }

    public static String encode(final BlockPos position) {
        return Long.toString(position.asLong());
    }

    public static long decode(final String key) {
        final Matcher printed = PRINTED_POSITION.matcher(key);
        if (printed.matches()) {
            return new BlockPos(
                    Integer.parseInt(printed.group(1)),
                    Integer.parseInt(printed.group(2)),
                    Integer.parseInt(printed.group(3))).asLong();
        }
        return Long.parseLong(key);
    }
}
