package com.bettercontent.bettercompatfixes.compat;

import java.util.function.Predicate;

public final class OptionalTinkerSkinPolicy {
    private OptionalTinkerSkinPolicy() {}

    public static boolean skipSyntheticSkin(String item, Predicate<String> registered) {
        return ("constructs_casting:battlestaff".equals(item) || "constructs_casting:flamberge".equals(item))
                && !registered.test(item);
    }
}
