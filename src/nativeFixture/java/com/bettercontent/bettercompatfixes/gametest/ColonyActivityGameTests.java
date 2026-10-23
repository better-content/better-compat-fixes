package com.bettercontent.bettercompatfixes.gametest;

import com.accbdd.complicated_bees.block.entity.ApiaryBlockEntity;
import com.accbdd.complicated_bees.block.entity.BeeNestBlockEntity;
import com.accbdd.complicated_bees.bees.Species;
import com.accbdd.complicated_bees.registry.BlocksRegistration;
import com.accbdd.complicated_bees.registry.SpeciesRegistration;
import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.bees.ColonyActivity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ColonyActivityGameTests {
    @SubscribeEvent public static void register(RegisterGameTestsEvent event) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("complicated_bees")) event.register(ColonyActivityGameTests.class);
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void nativeEmptyApiaryActivityAndMinimalPacketDoNotMutateProduction(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlocksRegistration.APIARY.get());
        ApiaryBlockEntity apiary = (ApiaryBlockEntity) helper.getBlockEntity(new net.minecraft.core.BlockPos(1, 1, 1));
        helper.assertTrue(apiary instanceof ColonyActivity, "Native housing must actually receive required mixin");
        var before = apiary.saveWithoutMetadata();
        helper.assertTrue(!((ColonyActivity) apiary).betterCompat$colonyActive(), "Empty apiary cannot advertise work");
        var tag = apiary.getUpdateTag();
        helper.assertTrue(minimalInactiveCosmeticPacket(tag), "Only exact legacy/typed cosmetics may be synchronized, never native inventory/genetics");
        helper.assertTrue(!tag.getBoolean("better_compat_fixes:colony_active"), "Inactive packet bit");
        helper.assertTrue(apiary.getUpdatePacket() != null, "Native block update packet must exist");
        helper.assertTrue(before.equals(apiary.saveWithoutMetadata()), "Cosmetic observation must not change production state");
        helper.succeed();
    }

    private static boolean minimalInactiveCosmeticPacket(net.minecraft.nbt.CompoundTag tag) {
        if (!tag.getAllKeys().equals(java.util.Set.of("better_compat_fixes:colony_active", "better_compat_fixes:colony_appearance_v1"))
                || !tag.contains("better_compat_fixes:colony_active", net.minecraft.nbt.Tag.TAG_BYTE)
                || tag.getByte("better_compat_fixes:colony_active") != 0
                || !tag.contains("better_compat_fixes:colony_appearance_v1", net.minecraft.nbt.Tag.TAG_COMPOUND)) return false;
        var data = tag.getCompound("better_compat_fixes:colony_appearance_v1");
        return data.getAllKeys().equals(java.util.Set.of("version", "active", "known", "rgb"))
                && data.contains("version", net.minecraft.nbt.Tag.TAG_INT) && data.getInt("version") == 1
                && data.contains("active", net.minecraft.nbt.Tag.TAG_BYTE) && data.getByte("active") == 0
                && data.contains("known", net.minecraft.nbt.Tag.TAG_BYTE) && data.getByte("known") == 0
                && data.contains("rgb", net.minecraft.nbt.Tag.TAG_INT) && data.getInt("rgb") == 0;
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void nativeNestSpeciesDistinguishesOccupiedAndInvalid(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlocksRegistration.BEE_NEST.get());
        BeeNestBlockEntity nest = (BeeNestBlockEntity) helper.getBlockEntity(new net.minecraft.core.BlockPos(1, 1, 1));
        nest.setSpecies(Species.INVALID);
        helper.assertTrue(nest.getSpecies() == Species.INVALID, "Invalid nest has no colony");
        Species forest = SpeciesRegistration.getFromResourceLocation(new ResourceLocation("complicated_bees", "forest"));
        helper.assertTrue(forest != null && forest != Species.INVALID, "Pinned native species is registered");
        nest.setSpecies(forest);
        helper.assertTrue(nest.getSpecies() == forest, "Occupied nest is native species state");
        helper.assertTrue(nest.getUpdateTag().getString("species").equals("complicated_bees:forest"), "Native nest packet already carries occupancy");
        helper.succeed();
    }
}
