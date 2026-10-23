package com.bettercontent.bettercompatfixes.compat.dynamictrees;

/** Native final-root conversion accepts raw registered soil or an existing RootyBlock. */
public final class RootySoilPolicy {
    private RootySoilPolicy() {}
    public static boolean reject(boolean worldGen, boolean registered, boolean rooty) {
        return worldGen && !registered && !rooty;
    }
}
