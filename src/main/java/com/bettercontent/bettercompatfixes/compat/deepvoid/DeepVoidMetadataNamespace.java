package com.bettercontent.bettercompatfixes.compat.deepvoid;

/** Source SDK and production SRG bridges are mutually exclusive, never alias fallbacks. */
public final class DeepVoidMetadataNamespace {
    private DeepVoidMetadataNamespace() { }

    public static boolean applies(String mixinClassName, boolean production) {
        return switch (mixinClassName) {
            case "com.bettercontent.bettercompatfixes.mixin.deepvoid.TemplateMetadataAccessor",
                 "com.bettercontent.bettercompatfixes.mixin.deepvoid.RegionWriteRadiusAccessor",
                 "com.bettercontent.bettercompatfixes.mixin.deepvoid.IgnoreMetadataAccessor" -> !production;
            case "com.bettercontent.bettercompatfixes.mixin.deepvoid.TemplateMetadataSrgAccessor",
                 "com.bettercontent.bettercompatfixes.mixin.deepvoid.RegionWriteRadiusSrgAccessor",
                 "com.bettercontent.bettercompatfixes.mixin.deepvoid.IgnoreMetadataSrgAccessor" -> production;
            default -> true;
        };
    }
}
