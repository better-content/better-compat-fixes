package com.bettercontent.bettercompatfixes.config;

import com.bettercontent.bettercompatfixes.compat.LostCitiesC2meDhSerialization;
import com.bettercontent.bettercompatfixes.compat.TwilightForestMazeSerialization;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;

public final class BcFixesConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue DYNAMIC_TREES_SEASON_CONTEXT_CONCURRENT_MAP;
    public static final ForgeConfigSpec.BooleanValue DYNAMIC_TREES_UNEARTHED_REGOLITH_SOILS;
    public static final ForgeConfigSpec.BooleanValue DYNAMIC_TREES_DESTROY_UNSUPPORTED_TREES;
    public static final ForgeConfigSpec.BooleanValue DYNAMIC_TREES_SPECIES_HARDNESS;
    public static final ForgeConfigSpec.BooleanValue HYLE_SAFE_TERTIARY_SELECTION;
    public static final ForgeConfigSpec.BooleanValue HYLE_COMPLETE_BOTTOM_SECTION;
    public static final ForgeConfigSpec.BooleanValue HYLE_RUN_AFTER_UNDERGROUND_DECORATION;
    public static final ForgeConfigSpec.BooleanValue APOTHEOSIS_SKIP_OFF_THREAD_TOOLTIPS;
    public static final ForgeConfigSpec.BooleanValue ADVANCED_LOOT_INFO_SKIP_OFF_THREAD_EMI_REGISTRATION;
    public static final ForgeConfigSpec.BooleanValue AMBIENT_SOUNDS_RETRY_REJECTED_STREAMS;
    public static final ForgeConfigSpec.BooleanValue EXPLOSION_OVERHAUL_AUTO_ACCEPT_SCAN_PROMPTS;
    public static final ForgeConfigSpec.BooleanValue EXPLOSION_OVERHAUL_CLAMP_CONCUSSION_DURATION;
    public static final ForgeConfigSpec.IntValue EXPLOSION_OVERHAUL_MAX_CONCUSSION_DURATION_SECONDS;
    public static final ForgeConfigSpec.BooleanValue SGI_RERUN_HYLE_AFTER_SURFACE_CONFORM;
    public static final ForgeConfigSpec.BooleanValue LOST_CITIES_SERIALIZE_DH_C2ME_FEATURE_PLACEMENT;
    public static final ForgeConfigSpec.BooleanValue LOST_CITIES_CANCEL_STALE_DH_CLIENT_REQUESTS;
    public static final ForgeConfigSpec.BooleanValue TWILIGHT_FOREST_SERIALIZE_C2ME_MAZE_PLACEMENT;
    public static final ForgeConfigSpec.BooleanValue THE_FLESH_THAT_HATES_DISABLE_PROXIMITY_MUSIC;
    public static final ForgeConfigSpec.BooleanValue WEATHER2_DISABLE_FOG_OVERRIDE_WITH_SHADERS;
    public static final ForgeConfigSpec.BooleanValue SOPHISTICATED_STORAGE_BARREL_HOPPER_EXTRACTION;
    public static final ForgeConfigSpec.BooleanValue FLUID_MIXING_BLOCK_GENERATED_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue FARMLAND_PREVENT_TRAMPLE;
    public static final ForgeConfigSpec.BooleanValue MOBS_DISABLE_SUN_BURN_TICK;
    public static final ForgeConfigSpec.BooleanValue MOBS_BLOCK_NATURAL_SURFACE_HOSTILES;
    public static final ForgeConfigSpec.IntValue MOBS_NATURAL_SURFACE_DEPTH;
    public static final ForgeConfigSpec.BooleanValue MOBS_BLOCK_VANILLA_ZOMBIES_AND_SKELETONS;
    public static final ForgeConfigSpec.BooleanValue POLLUTION_DISABLE_PLAYER_BLOCK_BREAK_EMISSIONS;
    public static final ForgeConfigSpec.DoubleValue VANILLA_BOAT_DURABILITY_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue VANILLA_BOAT_SUPPRESS_DESTRUCTION_DROP;
    public static final ForgeConfigSpec.BooleanValue TOGGLE_SNEAK;
    public static final ForgeConfigSpec.BooleanValue CREATIVE_INVENTORY_HIDE_SEARCH_TAB;
    public static final ForgeConfigSpec.BooleanValue SLEEPING_OVERHAUL_PACE_TIMELAPSE;
    public static final ForgeConfigSpec.IntValue SLEEPING_OVERHAUL_TARGET_TICKS_PER_SECOND;
    public static final ForgeConfigSpec.BooleanValue SLEEPING_OVERHAUL_INTERRUPT_IMMEDIATE_DANGER;
    public static final ForgeConfigSpec.DoubleValue ITEMS_EXTRA_PICKUP_HORIZONTAL_RADIUS;
    public static final ForgeConfigSpec.DoubleValue VEGETATION_DECORATIVE_TRAMPLE_CHANCE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("controls");
        TOGGLE_SNEAK = builder
                .comment(
                        "Enables the custom press-to-toggle sneak behavior.",
                        "When disabled, vanilla hold-to-sneak input remains active and the custom toggle handler is inert.")
                .define("toggleSneak", false);
        builder.pop();

        builder.push("creativeInventory");
        CREATIVE_INVENTORY_HIDE_SEARCH_TAB = builder
                .comment(
                        "Hides only Minecraft's Creative inventory search tab.",
                        "JEI, EMI, item contents, and every other Creative tab remain available.")
                .define("hideSearchTab", true);
        builder.pop();

        builder.push("sleepingOverhaul");
        SLEEPING_OVERHAUL_PACE_TIMELAPSE = builder
                .comment("Paces Sleeping Overhaul's simulated ticks instead of allowing an effectively instant night.")
                .define("paceTimelapse", true);
        SLEEPING_OVERHAUL_TARGET_TICKS_PER_SECOND = builder
                .comment("Target simulated ticks per real second while Sleeping Overhaul timelapse is active.")
                .defineInRange("targetTicksPerSecond", 800, 20, 2000);
        SLEEPING_OVERHAUL_INTERRUPT_IMMEDIATE_DANGER = builder
                .comment("Stops only an active Sleeping Overhaul timelapse when a sleeping player takes damage or has a nearby hostile targeting them.",
                        "World time, food, machines, weather, and ordinary server ticks remain owned by their normal systems.")
                .define("interruptImmediateDanger", true);
        builder.pop();

        builder.push("items");
        ITEMS_EXTRA_PICKUP_HORIZONTAL_RADIUS = builder
                .comment("Extra horizontal item-pickup radius in blocks. Vanilla vertical reach is unchanged.")
                .defineInRange("extraPickupHorizontalRadius", 1.0D, 0.0D, 8.0D);
        builder.pop();

        builder.push("vegetation");
        VEGETATION_DECORATIVE_TRAMPLE_CHANCE = builder
                .comment("Chance to remove vanilla grass or tall grass once per distinct block entry by a moving living entity.")
                .defineInRange("decorativeTrampleChance", 0.10D, 0.0D, 1.0D);
        builder.pop();



        builder.push("dynamicTrees");
        DYNAMIC_TREES_SEASON_CONTEXT_CONCURRENT_MAP = builder
                .comment(
                        "Replaces Dynamic Trees' NormalSeasonManager season context HashMap with a ConcurrentHashMap.",
                        "This is intended to prevent ConcurrentModificationException during parallel feature generation, especially with C2ME threaded features.",
                        "Disable this if Dynamic Trees changes this internals or if diagnosing unrelated season behavior.")
                .define("seasonContextConcurrentMap", true);
        DYNAMIC_TREES_UNEARTHED_REGOLITH_SOILS = builder
                .comment(
                        "Registers Unearthed/Hyle regolith and overgrown stone surface blocks as Dynamic Trees dirt-like soils.",
                        "Keeps Unearthed dirt replacement enabled while allowing Dynamic Trees worldgen to place forest trees on replaced forest surfaces.",
                        "Disable only when diagnosing Dynamic Trees soil registration behavior.")
                .define("unearthedRegolithSoils", true);
        DYNAMIC_TREES_DESTROY_UNSUPPORTED_TREES = builder
                .comment(
                        "Opt-in administrative repair for unsupported Dynamic Trees only.",
                        "Disabled by default because its chunk-load scan can alter an already-generated world.",
                        "Enable only after a verified backup and an operator-approved maintenance window.")
                .define("destroyUnsupportedTrees", false);
        DYNAMIC_TREES_SPECIES_HARDNESS = builder
                .comment(
                        "Makes each Dynamic Trees branch use the destroy hardness of its family's native primitive log.",
                        "This preserves wood-species mining differences shared with native log felling instead of Dynamic Trees' global 2.0 hardness.",
                        "Disable to restore Dynamic Trees' default branch hardness.")
                .define("speciesHardness", true);
        builder.pop();

        builder.push("pollution");
        POLLUTION_DISABLE_PLAYER_BLOCK_BREAK_EMISSIONS = builder
                .comment(
                        "Cancels Pollution of the Realms emissions from its player block-break event only.",
                        "World-level block-break emissions remain available to Create drills, contraptions, explosions, and other automation.")
                .define("disablePlayerBlockBreakEmissions", true);
        builder.pop();

        builder.push("vanillaBoats");
        VANILLA_BOAT_DURABILITY_MULTIPLIER = builder
                .comment(
                        "Multiplier applied to the accumulated-damage destruction threshold of vanilla boats and chest boats.",
                        "Movement, collisions, passengers, damage accumulation, and modded vessel entity types are unchanged.")
                .defineInRange("durabilityMultiplier", 10.0D, 1.0D, 100.0D);
        VANILLA_BOAT_SUPPRESS_DESTRUCTION_DROP = builder
                .comment(
                        "Suppresses the boat or chest-boat item when a vanilla vessel is destroyed.",
                        "Chest-boat inventory contents still drop normally; unrelated entity drops are unchanged.")
                .define("suppressDestructionDrop", true);
        builder.pop();

        builder.push("hyle");
        HYLE_SAFE_TERTIARY_SELECTION = builder
                .comment(
                        "Guards Hyle tertiary stone selection against invalid indices.",
                        "Prevents worldgen crashes when Hyle's tertiary selector requests an index outside the available tertiary list size.",
                        "Keeps Unearthed/Hyle stone replacement enabled while avoiding ArrayIndexOutOfBoundsException in RoughNoiseSampler.selectTertiary.")
                .define("safeTertiarySelection", true);
        HYLE_COMPLETE_BOTTOM_SECTION = builder
                .comment(
                        "Completes Hyle stone replacement in the Overworld's lowest chunk section.",
                        "Hyle's bottom interpolation slice can resolve to its no-replacement sentinel, leaving deepslate and tuff below Y -48 untouched.",
                        "The repair uses the nearest valid generated Hyle stratum for each column, so regional Unearthed geology remains continuous.")
                .define("completeBottomSection", true);
        HYLE_RUN_AFTER_UNDERGROUND_DECORATION = builder
                .comment(
                        "Moves Hyle's stone replacement pass to the end of underground decoration.",
                        "This replaces stone-family blocks emitted after local modifications by underground structures, ores, and decoration features.",
                        "Above-ground authored boulders and structure masonry remain outside this underground geology pass.")
                .define("runAfterUndergroundDecoration", true);
        builder.pop();

        builder.push("apotheosis");
        APOTHEOSIS_SKIP_OFF_THREAD_TOOLTIPS = builder
                .comment(
                        "Skips Apotheosis' client tooltip augmentation when another mod computes tooltips off the Minecraft client thread.",
                        "This prevents C2ME's safe-random guard from reporting off-thread world random access during EMI tooltip indexing.",
                        "Normal render-thread tooltips are left unchanged.")
                .define("skipOffThreadTooltips", true);
        builder.pop();

        builder.push("advancedLootInfo");
        ADVANCED_LOOT_INFO_SKIP_OFF_THREAD_EMI_REGISTRATION = builder
                .comment(
                        "Skips Advanced Loot Info's EMI data registration when EMI runs it off the Minecraft client thread.",
                        "ALI creates example entities during EMI indexing; some entity constructors touch world random and trip C2ME's safe-random guard off-thread.",
                        "Normal render-thread ALI behavior is left unchanged.")
                .define("skipOffThreadEmiRegistration", true);
        builder.pop();

        builder.push("ambientSounds");
        AMBIENT_SOUNDS_RETRY_REJECTED_STREAMS = builder
                .comment(
                        "Retires AmbientSounds streams that Minecraft rejected before playback so their parent sounds can retry later.",
                        "Minecraft has a finite sound-channel pool; without this recovery, a rejected stream can remain tracked forever and permanently silence that ambient sound.",
                        "Active streams and streams that have played at least once retain AmbientSounds' normal lifecycle.")
                .define("retryRejectedStreams", true);
        builder.pop();

        builder.push("explosionOverhaul");
        EXPLOSION_OVERHAUL_AUTO_ACCEPT_SCAN_PROMPTS = builder
                .comment(
                        "Automatically accepts Explosion Overhaul's chunk-index prompts without displaying their HUD.",
                        "Existing scan data is loaded when available; otherwise a new scan starts, matching the prompt's affirmative choice.")
                .define("autoAcceptScanPrompts", true);
        EXPLOSION_OVERHAUL_CLAMP_CONCUSSION_DURATION = builder
                .comment(
                        "Caps Explosion Overhaul concussion effects so large clustered blasts cannot cause excessively long shell shock.",
                        "The cap applies to blur, camera sway, low-pass audio, and deafness, including duration accumulated by repeated blasts.")
                .define("clampConcussionDuration", true);
        EXPLOSION_OVERHAUL_MAX_CONCUSSION_DURATION_SECONDS = builder
                .comment("Maximum concussion hold duration in seconds. Default: 45 seconds.")
                .defineInRange("maxConcussionDurationSeconds", 45, 1, 100);
        builder.pop();



        builder.push("fluidMixing");
        FLUID_MIXING_BLOCK_GENERATED_BLOCKS = builder
                .comment(
                        "Reverts fluid-generated block placements unless the produced block is explicitly allowlisted.",
                        "This preserves the current pack policy from Cobble-Gen-Haters while moving ownership intobetter_compat_fixes.",
                        "The default allowlist is data-driven through thebetter_compat_fixes:allowed_fluid_generated_blocks block tag.")
                .define("blockGeneratedBlocks", true);
        builder.pop();

        builder.push("farmland");
        FARMLAND_PREVENT_TRAMPLE = builder
                .comment(
                        "Cancels farmland trampling from fall impacts.",
                        "Prevents players and mobs from degrading cultivated soil back into dirt when landing on it.",
                        "Disable only if a future system intentionally wants fall-impact farmland loss back.")
                .define("preventTrample", true);
        builder.pop();

        builder.push("mobs");
        MOBS_DISABLE_SUN_BURN_TICK = builder
                .comment(
                        "Forces Mob.isSunBurnTick() to return false for protected mobs.",
                        "Phantoms and Phantom subclasses retain vanilla daylight burning so exposed phantoms are cleared after sunrise.",
                        "Other daylight-sensitive mobs remain protected; ordinary fire and non-solar fire damage are unchanged.")
                .define("disableSunBurnTick", true);
        MOBS_BLOCK_NATURAL_SURFACE_HOSTILES = builder
                .comment(
                        "Denies natural and chunk-generation monsters near the Overworld terrain surface and on tagged grass-covered ground at any depth.",
                        "The surface is measured with the leaf-ignoring motion-blocking heightmap, so tree canopies do not create ambient spawn pockets.",
                        "The depth-independent ground list is data-driven through the better_compat_fixes:ambient_spawn_denied_surfaces block tag.",
                        "Spawner, structure, event, summon, command, and scripted entity insertion remain unaffected.")
                .define("blockNaturalSurfaceHostiles", true);
        MOBS_NATURAL_SURFACE_DEPTH = builder
                .comment(
                        "Number of blocks below the local leaf-ignoring terrain surface that remain reserved from ambient monster spawning.",
                        "A value of 6 denies the surface block and the six-block band beneath it while leaving deeper caves active.")
                .defineInRange("naturalSurfaceDepth", 6, 0, 64);
        MOBS_BLOCK_VANILLA_ZOMBIES_AND_SKELETONS = builder
                .comment(
                        "Denies only natural and chunk-generation spawning of minecraft:zombie and minecraft:skeleton in the Overworld.",
                        "Spawner, structure, event, summon, command, scripted insertion, and variant entity types remain available.")
                .define("blockVanillaZombiesAndSkeletons", true);
        builder.pop();

        builder.push("structureGenerationImprover");
        SGI_RERUN_HYLE_AFTER_SURFACE_CONFORM = builder
                .comment(
                        "Translates Structure Generation Improver's vanilla rock writes to the nearby Hyle/Unearthed stone palette.",
                        "SGI writes new vanilla terrain after normal biome decoration, which means Hyle/Unearthed has already run and cannot replace those blocks.",
                        "The legacy key name is retained for compatibility; this now replaces writes directly without rerunning Hyle or scanning the full chunk.")
                .define("rerunHyleAfterSurfaceConform", true);
        builder.pop();

        builder.push("lostCities");
        LOST_CITIES_SERIALIZE_DH_C2ME_FEATURE_PLACEMENT = builder
                .comment(
                        "Serializes Lost Cities feature placement only in the lostcities:lostcity dimension when Lost Cities and C2ME are loaded.",
                        "This keeps Lost Cities structures and C2ME threaded generation active elsewhere while avoiding shared Lost Cities generation-state races on C2ME workers.",
                        "The legacy key name is retained for config compatibility; Distant Horizons is not required.",
                        "Disable only when diagnosing Lost Cities/C2ME compatibility behavior.")
                .define("serializeDhC2meFeaturePlacement", true);
        LOST_CITIES_CANCEL_STALE_DH_CLIENT_REQUESTS = builder
                .comment(
                        "Cancels stale Distant Horizons client full-data requests rejected during Lost Cities dimension switches.",
                        "Distant Horizons 2.4.5 can complete these rejected requests as failed results with a null Throwable, then log a NullPointerException in onWorldGenTaskComplete.",
                        "This does not disable DH generation; it drops only the already-rejected client request future.")
                .define("cancelStaleDhClientRequests", true);
        builder.pop();

        builder.push("twilightForest");
        TWILIGHT_FOREST_SERIALIZE_C2ME_MAZE_PLACEMENT = builder
                .comment(
                        "Serializes placement only among Twilight Forest mazes that share one random source when C2ME is loaded.",
                        "This prevents parallel Minotaur Maze chunks from tripping LegacyRandomSource's threading detector without serializing unrelated world generation.")
                .define("serializeC2meMazePlacement", true);
        builder.pop();

        builder.push("theFleshThatHates");
        THE_FLESH_THAT_HATES_DISABLE_PROXIMITY_MUSIC = builder
                .comment(
                        "Disables The Flesh That Hates' automatic proximity music near clusters of flesh blocks.",
                        "The upstream handler pauses vanilla music and routes its horror score through the Jukebox/Note Blocks channel.",
                        "Entity, combat, evolution, jukebox, note-block, and other Records-channel sounds remain unchanged.")
                .define("disableProximityMusic", true);
        builder.pop();

        builder.push("weather2");
        WEATHER2_DISABLE_FOG_OVERRIDE_WITH_SHADERS = builder
                .comment(
                        "Disables Weather2's custom fog color and distance override while an Oculus shader pack is active.",
                        "Weather2 still tracks storm state and renders weather particles; the active shader pack owns sky and fog rendering.",
                        "Shaders-off Weather2 fog behavior is unchanged.")
                .define("disableFogOverrideWithShaders", true);
        builder.pop();

        builder.push("sophisticatedStorage");
        SOPHISTICATED_STORAGE_BARREL_HOPPER_EXTRACTION = builder
                .comment(
                        "Allows vanilla hoppers below Sophisticated Storage barrels and limited barrels to extract items.",
                        "Extraction uses the barrel's input/output inventory handler, preserving storage filters and slot rules.",
                        "Other Sophisticated Storage blocks and hopper insertion behavior are unchanged.")
                .define("barrelHopperExtraction", true);
        builder.pop();



        SPEC = builder.build();
    }

    private BcFixesConfig() {
    }

    public static boolean dynamicTreesSeasonContextConcurrentMap() {
        return isLoaded("dynamictrees") && (!SPEC.isLoaded() || DYNAMIC_TREES_SEASON_CONTEXT_CONCURRENT_MAP.get());
    }

    public static boolean dynamicTreesUnearthedRegolithSoils() {
        return DYNAMIC_TREES_UNEARTHED_REGOLITH_SOILS.get() && isLoaded("dynamictrees") && isLoaded("unearthed");
    }

    public static boolean dynamicTreesDestroyUnsupportedTrees() {
        return DYNAMIC_TREES_DESTROY_UNSUPPORTED_TREES.get() && isLoaded("dynamictrees");
    }

    public static boolean dynamicTreesSpeciesHardness() {
        return DYNAMIC_TREES_SPECIES_HARDNESS.get() && isLoaded("dynamictrees");
    }

    public static boolean hyleSafeTertiarySelection() {
        return isLoaded("hyle") && HYLE_SAFE_TERTIARY_SELECTION.get();
    }

    public static boolean hyleCompleteBottomSection() {
        return isLoaded("hyle") && HYLE_COMPLETE_BOTTOM_SECTION.get();
    }

    public static boolean hyleRunAfterUndergroundDecoration() {
        return isLoaded("hyle") && HYLE_RUN_AFTER_UNDERGROUND_DECORATION.get();
    }

    public static boolean apotheosisSkipOffThreadTooltips() {
        return isLoaded("apotheosis") && APOTHEOSIS_SKIP_OFF_THREAD_TOOLTIPS.get();
    }

    public static boolean advancedLootInfoSkipOffThreadEmiRegistration() {
        return isLoaded("ali") && isLoaded("emi") && ADVANCED_LOOT_INFO_SKIP_OFF_THREAD_EMI_REGISTRATION.get();
    }

    public static boolean ambientSoundsRetryRejectedStreams() {
        return isLoaded("ambientsounds") && AMBIENT_SOUNDS_RETRY_REJECTED_STREAMS.get();
    }

    public static boolean explosionOverhaulClampConcussionDuration() {
        return isLoaded("explosionoverhaul") && EXPLOSION_OVERHAUL_CLAMP_CONCUSSION_DURATION.get();
    }

    public static boolean explosionOverhaulAutoAcceptScanPrompts() {
        return isLoaded("explosionoverhaul") && EXPLOSION_OVERHAUL_AUTO_ACCEPT_SCAN_PROMPTS.get();
    }

    public static int explosionOverhaulMaxConcussionDurationSeconds() {
        return EXPLOSION_OVERHAUL_MAX_CONCUSSION_DURATION_SECONDS.get();
    }

    public static boolean fluidMixingBlockGeneratedBlocks() {
        return FLUID_MIXING_BLOCK_GENERATED_BLOCKS.get();
    }

    public static boolean farmlandPreventTrample() {
        return FARMLAND_PREVENT_TRAMPLE.get();
    }

    public static boolean mobsDisableSunBurnTick() {
        return MOBS_DISABLE_SUN_BURN_TICK.get();
    }

    public static boolean mobsBlockNaturalSurfaceHostiles() {
        return MOBS_BLOCK_NATURAL_SURFACE_HOSTILES.get();
    }

    public static int mobsNaturalSurfaceDepth() {
        return MOBS_NATURAL_SURFACE_DEPTH.get();
    }

    public static boolean mobsBlockVanillaZombiesAndSkeletons() {
        return MOBS_BLOCK_VANILLA_ZOMBIES_AND_SKELETONS.get();
    }

    public static boolean pollutionDisablePlayerBlockBreakEmissions() {
        return isLoaded("adpother") && POLLUTION_DISABLE_PLAYER_BLOCK_BREAK_EMISSIONS.get();
    }

    public static double vanillaBoatDurabilityMultiplier() {
        return VANILLA_BOAT_DURABILITY_MULTIPLIER.get();
    }

    public static boolean vanillaBoatSuppressDestructionDrop() {
        return VANILLA_BOAT_SUPPRESS_DESTRUCTION_DROP.get();
    }

    public static boolean toggleSneak() {
        return TOGGLE_SNEAK.get();
    }

    public static boolean hideCreativeInventorySearchTab() {
        return CREATIVE_INVENTORY_HIDE_SEARCH_TAB.get();
    }

    public static boolean sleepingOverhaulPaceTimelapse() {
        return SLEEPING_OVERHAUL_PACE_TIMELAPSE.get();
    }

    public static int sleepingOverhaulTargetTicksPerSecond() {
        return SLEEPING_OVERHAUL_TARGET_TICKS_PER_SECOND.get();
    }

    public static boolean sleepingOverhaulInterruptImmediateDanger() {
        return SLEEPING_OVERHAUL_INTERRUPT_IMMEDIATE_DANGER.get();
    }

    public static double itemsExtraPickupHorizontalRadius() {
        return ITEMS_EXTRA_PICKUP_HORIZONTAL_RADIUS.get();
    }

    public static double vegetationDecorativeTrampleChance() {
        return VEGETATION_DECORATIVE_TRAMPLE_CHANCE.get();
    }

    public static boolean sgiRerunHyleAfterSurfaceConform() {
        return SGI_RERUN_HYLE_AFTER_SURFACE_CONFORM.get()
                && isLoaded("structure_generation_improver")
                && isLoaded("hyle")
                && isLoaded("unearthed");
    }

    public static boolean lostCitiesSerializeDhC2meFeaturePlacement() {
        return LostCitiesC2meDhSerialization.dependenciesAvailable(
                LOST_CITIES_SERIALIZE_DH_C2ME_FEATURE_PLACEMENT.get(),
                BcFixesConfig::isLoaded);
    }

    public static boolean lostCitiesCancelStaleDhClientRequests() {
        return LOST_CITIES_CANCEL_STALE_DH_CLIENT_REQUESTS.get()
                && isLoaded("lostcities")
                && isLoaded("distanthorizons")
                && isLoaded("c2me");
    }

    public static boolean twilightForestSerializeC2meMazePlacement() {
        return TwilightForestMazeSerialization.dependenciesAvailable(
                TWILIGHT_FOREST_SERIALIZE_C2ME_MAZE_PLACEMENT.get(),
                BcFixesConfig::isLoaded);
    }

    public static boolean theFleshThatHatesDisableProximityMusic() {
        return THE_FLESH_THAT_HATES_DISABLE_PROXIMITY_MUSIC.get()
                && isLoaded("the_flesh_that_hates");
    }

    public static boolean weather2DisableFogOverrideWithShaders() {
        return WEATHER2_DISABLE_FOG_OVERRIDE_WITH_SHADERS.get()
                && isLoaded("weather2")
                && isLoaded("oculus");
    }

    public static boolean sophisticatedStorageBarrelHopperExtraction() {
        return SOPHISTICATED_STORAGE_BARREL_HOPPER_EXTRACTION.get()
                && isLoaded("sophisticatedstorage");
    }

    private static boolean isLoaded(final String modId) {
        return ModList.get().isLoaded(modId);
    }
}
