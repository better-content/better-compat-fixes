package com.bettercontent.bettercompatfixes.compat;

/** Exact beta-5 eye models: ClassLoader resource names are relative and case-sensitive. */
public final class DragonEyesModelPaths {
    private DragonEyesModelPaths() {}

    public static String classLoaderPath(String path) {
        return switch (path) {
            case "/assets/iceandfire/models/tabula/firedragon/firedragon_Ground.tbl" ->
                    "assets/iceandfire/models/tabula/firedragon/firedragon_ground.tbl";
            case "/assets/iceandfire/models/tabula/icedragon/icedragon_Ground.tbl" ->
                    "assets/iceandfire/models/tabula/icedragon/icedragon_ground.tbl";
            case "/assets/iceandfire/models/tabula/lightningdragon/lightningdragon_Ground.tbl" ->
                    "assets/iceandfire/models/tabula/lightningdragon/lightningdragon_ground.tbl";
            default -> path;
        };
    }
}
