package com.bettercontent.bettercontentfixes.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class BetterContentMixinPlugin implements IMixinConfigPlugin {
    private static final String BETTER_CAVES_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.bettercaves.";
    private static final String CRAFTING_STATION_POLYMORPH_MIXIN =
            "com.bettercontent.bettercontentfixes.mixin.tconstruct.CraftingStationPolymorphMixin";
    private static final String EPIC_FIGHT_VS_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.epicfightvs.";
    private static final String EPIC_FIGHT_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.epicfight.";
    private static final String EPIC_FIGHT_CONTROLIFY_MIXIN = EPIC_FIGHT_MIXIN_PREFIX
            + "EpicFightControlifyEntrypointMixin";
    private static final String EPIC_FIGHT_FIRST_PERSON_RENDERER_MIXIN = EPIC_FIGHT_MIXIN_PREFIX
            + "FirstPersonRendererMixin";
    private static final String EPIC_FIGHT_FIRST_PERSON_ARMOR_MIXIN = EPIC_FIGHT_MIXIN_PREFIX
            + "WearableItemLayerMixin";
    private static final String FALLOUT_WASTELANDS_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.falloutwastelands.";
    private static final String TWILIGHT_FOREST_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.twilightforest.";
    private static final String PARCOOL_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.parcool.";
    private static final String ALI_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.ali.";
    private static final String APOTHEOSIS_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.apotheosis.";
    private static final String EXPLOSION_OVERHAUL_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.explosionoverhaul.";
    private static final String PATCHOULI_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.patchouli.";
    private static final String PNEUMATICCRAFT_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.pneumaticcraft.";
    private static final String PNEUMATICCRAFT_TCON_HEAD_MIXIN_PREFIX =
            PNEUMATICCRAFT_MIXIN_PREFIX + "tconhead.";
    private static final String COMPLICATED_BEES_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.complicatedbees.";
    private static final String ADPOTHER_COLD_SWEAT_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.adpother.coldsweat.";
    private static final String ADPOTHER_LITTLE_LOGISTICS_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.adpother.littlelogistics.";
    private static final String ADPOTHER_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.adpother.";
    private static final String SODIUM_DYNAMIC_LIGHTS_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.sodiumdynamiclights.";
    private static final String VALKYRIEN_SKIES_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.valkyrienskies.";
    private static final String JSON_THINGS_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.jsonthings.";
    private static final String THIRST_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.thirst.";
    private static final String KUBEJS_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.kubejs.";
    private static final String DYNAMIC_TREES_POISSON_MIXIN =
            "com.bettercontent.bettercontentfixes.mixin.dynamictrees.LevelPoissonDiscProviderMixin";
    private static final String DYNAMIC_TREES_BRANCH_HARDNESS_MIXIN =
            "com.bettercontent.bettercontentfixes.mixin.dynamictrees.BranchHardnessMixin";
    private static final String DTAETHER_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.dtaether.";
    private static final String CREATE_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.create.";
    private static final String CREATE_SIFTER_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.createsifter.";
    private static final String HYLE_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.hyle.";
    private static final String LOST_CITIES_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.lostcities.";
    private static final String LOST_CITIES_SECTION_BOUNDS_MIXIN = LOST_CITIES_MIXIN_PREFIX
            + "ChunkDriverMixin";
    private static final String POLLUTION_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.pollution.";
    private static final String SGI_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.sgi.";
    private static final String DISTANT_HORIZONS_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.distanthorizons.";
    private static final String SOPHISTICATED_STORAGE_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.sophisticatedstorage.";
    private static final String SOPHISTICATED_BACKPACKS_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.sophisticatedbackpacks.";
    private static final String SLEEPING_OVERHAUL_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.sleepingoverhaul.";
    private static final String TACZ_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.tacz.";
    private static final String FLESH_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.thefleshthathates.";
    private static final String CATACLYSM_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.cataclysm.";
    private static final String UNTAMED_MIXIN_PREFIX =
            "com.bettercontent.bettercontentfixes.mixin.untamedwilds.";

    @Override
    public void onLoad(final String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        final LoadingModList mods = FMLLoader.getLoadingModList();
        if (mixinClassName.startsWith(FLESH_MIXIN_PREFIX)) {
            return hasVersion(mods, "the_flesh_that_hates", "0.4");
        }
        if (mixinClassName.startsWith(CATACLYSM_MIXIN_PREFIX)) {
            return hasVersion(mods, "cataclysm", "3.31");
        }
        if (mixinClassName.startsWith(UNTAMED_MIXIN_PREFIX)) {
            return hasVersion(mods, "untamedwilds", "4.0.4");
        }
                        if (mixinClassName.startsWith(PNEUMATICCRAFT_TCON_HEAD_MIXIN_PREFIX)) {
            return hasMods(mods, "pneumaticcraft", "tconstruct");
        }
        if (mixinClassName.startsWith(BETTER_CAVES_MIXIN_PREFIX)) {
            return hasVersion(mods, "bettercaves", "1.20.1-Forge-2.0.6");
        }
        if (CRAFTING_STATION_POLYMORPH_MIXIN.equals(mixinClassName)) {
            return mods != null
                    && mods.getModFileById("tconstruct") != null
                    && mods.getModFileById("polymorph") != null;
        }
        if (mixinClassName.startsWith(EPIC_FIGHT_VS_MIXIN_PREFIX)) {
            return mods != null
                    && mods.getModFileById("epicfight") != null
                    && mods.getModFileById("valkyrienskies") != null;
        }
        if (EPIC_FIGHT_CONTROLIFY_MIXIN.equals(mixinClassName)) {
            return mods != null
                    && mods.getModFileById("epicfight") != null
                    && mods.getModFileById("controlify") != null;
        }
        if (EPIC_FIGHT_FIRST_PERSON_RENDERER_MIXIN.equals(mixinClassName)
                || EPIC_FIGHT_FIRST_PERSON_ARMOR_MIXIN.equals(mixinClassName)) {
            return hasVersion(mods, "epicfight", "20.14.17");
        }
        if (mixinClassName.startsWith(FALLOUT_WASTELANDS_MIXIN_PREFIX)) {
            return hasVersion(mods, "fallout_wastelands_", "1.0.0");
        }
        if (mixinClassName.startsWith(TWILIGHT_FOREST_MIXIN_PREFIX)) {
            return hasVersion(mods, "twilightforest", "4.3.2508")
                    && mods != null
                    && mods.getModFileById("c2me") != null;
        }
        if (mixinClassName.startsWith(PARCOOL_MIXIN_PREFIX)) {
            return hasVersion(mods, "parcool", "3.4.3.3");
        }
        if (mixinClassName.startsWith(EPIC_FIGHT_MIXIN_PREFIX)) {
            return mods != null && mods.getModFileById("epicfight") != null;
        }
                if (mixinClassName.startsWith(ALI_MIXIN_PREFIX)) {
            return mods != null && mods.getModFileById("ali") != null;
        }
        if (mixinClassName.startsWith(APOTHEOSIS_MIXIN_PREFIX)) {
            return mods != null && mods.getModFileById("apotheosis") != null;
        }
        if (mixinClassName.startsWith(EXPLOSION_OVERHAUL_MIXIN_PREFIX)) {
            return hasVersion(mods, "explosionoverhaul", "0.2.3.0-forge");
        }
        if (mixinClassName.startsWith(PATCHOULI_MIXIN_PREFIX)) {
            return mods != null && mods.getModFileById("patchouli") != null;
        }
        if (mixinClassName.startsWith(PNEUMATICCRAFT_MIXIN_PREFIX)) {
            return mods != null && mods.getModFileById("pneumaticcraft") != null;
        }
        if (mixinClassName.startsWith(COMPLICATED_BEES_MIXIN_PREFIX)) {
            return mods != null && mods.getModFileById("complicated_bees") != null;
        }
        if (mixinClassName.startsWith(ADPOTHER_COLD_SWEAT_MIXIN_PREFIX)) {
            return hasVersion(mods, "adpother", "8.1.49.0")
                    && hasVersion(mods, "cold_sweat", "2.4");
        }
        if (mixinClassName.startsWith(ADPOTHER_LITTLE_LOGISTICS_MIXIN_PREFIX)) {
            return hasVersion(mods, "adpother", "8.1.49.0")
                    && hasVersion(mods, "littlelogistics", "1.20.1.2");
        }
        if (mixinClassName.startsWith(ADPOTHER_MIXIN_PREFIX)) {
            return hasVersion(mods, "adpother", "8.1.49.0");
        }
        if (mixinClassName.startsWith(SODIUM_DYNAMIC_LIGHTS_MIXIN_PREFIX)) {
            return hasVersion(mods, "sodiumdynamiclights", "1.0.9");
        }
        if (mixinClassName.startsWith(VALKYRIEN_SKIES_MIXIN_PREFIX)) {
            return hasVersion(mods, "valkyrienskies", "2.4.11");
        }
        if (mixinClassName.startsWith(JSON_THINGS_MIXIN_PREFIX)) {
            return hasVersion(mods, "jsonthings", "0.9.13");
        }
                        if (mixinClassName.startsWith(THIRST_MIXIN_PREFIX)) {
            return hasVersion(mods, "thirst", "1.20.1-1.4.0");
        }
                if (mixinClassName.startsWith(KUBEJS_MIXIN_PREFIX)) {
            return hasVersion(mods, "kubejs", "2001.6.5-build.16");
        }
        if (DYNAMIC_TREES_POISSON_MIXIN.equals(mixinClassName)) {
            return hasVersion(mods, "dynamictrees", "1.20.1-1.4.10");
        }
        if (DYNAMIC_TREES_BRANCH_HARDNESS_MIXIN.equals(mixinClassName)) {
            return hasVersion(mods, "dynamictrees", "1.20.1-1.4.10");
        }
        if (mixinClassName.startsWith(DTAETHER_MIXIN_PREFIX)) {
            return hasVersion(mods, "dtaether", "1.20.1-1.3.3");
        }
        if (mixinClassName.startsWith(CREATE_MIXIN_PREFIX)) {
            return mods != null && mods.getModFileById("create") != null;
        }
        if (mixinClassName.startsWith(CREATE_SIFTER_MIXIN_PREFIX)) {
            return hasVersion(mods, "createsifter", "1.20.1-1.8.6-6.0.6");
        }
        if (mixinClassName.startsWith(HYLE_MIXIN_PREFIX)) {
            return mods != null && mods.getModFileById("hyle") != null;
        }
        if (LOST_CITIES_SECTION_BOUNDS_MIXIN.equals(mixinClassName)) {
            return hasVersion(mods, "lostcities", "1.20-7.4.11");
        }
        if (mixinClassName.startsWith(LOST_CITIES_MIXIN_PREFIX)) {
            return mods != null && mods.getModFileById("lostcities") != null;
        }
        if (mixinClassName.startsWith(POLLUTION_MIXIN_PREFIX)) {
            return mods != null && mods.getModFileById("adpother") != null;
        }
        if (mixinClassName.startsWith(SGI_MIXIN_PREFIX)) {
            return hasVersion(mods, "structure_generation_improver", "1.0.0");
        }
        if (mixinClassName.startsWith(DISTANT_HORIZONS_MIXIN_PREFIX)) {
            return hasVersion(mods, "distanthorizons", "2.4.5-b");
        }
        if (mixinClassName.startsWith(SOPHISTICATED_STORAGE_MIXIN_PREFIX)) {
            return mods != null && mods.getModFileById("sophisticatedstorage") != null;
        }
        if (mixinClassName.startsWith(SOPHISTICATED_BACKPACKS_MIXIN_PREFIX)) {
            return mods != null && mods.getModFileById("sophisticatedbackpacks") != null;
        }
        if (mixinClassName.startsWith(SLEEPING_OVERHAUL_MIXIN_PREFIX)) {
            return hasVersion(mods, "sleepingoverhaul", "2.1.0-Forge-1.20.1");
        }
        if (mixinClassName.startsWith(TACZ_MIXIN_PREFIX)) {
            return hasVersion(mods, "tacz", "1.20.1-1.1.8-hotfix");
        }
        return true;
    }

    private static boolean hasVersion(
            final LoadingModList mods,
            final String modId,
            final String expectedVersion
    ) {
        if (mods == null || mods.getModFileById(modId) == null) {
            return false;
        }
        return mods.getModFileById(modId).getMods().stream()
                .anyMatch(mod -> modId.equals(mod.getModId())
                        && expectedVersion.equals(mod.getVersion().toString()));
    }

    private static boolean hasMods(final LoadingModList mods, final String... modIds) {
        if (mods == null) return false;
        for (String modId : modIds) {
            if (mods.getModFileById(modId) == null) return false;
        }
        return true;
    }

    @Override
    public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
            final String targetClassName,
            final ClassNode targetClass,
            final String mixinClassName,
            final IMixinInfo mixinInfo
    ) {
    }

    @Override
    public void postApply(
            final String targetClassName,
            final ClassNode targetClass,
            final String mixinClassName,
            final IMixinInfo mixinInfo
    ) {
    }
}
