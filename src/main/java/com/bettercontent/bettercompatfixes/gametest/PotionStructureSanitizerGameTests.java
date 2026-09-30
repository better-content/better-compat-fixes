package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class PotionStructureSanitizerGameTests {
    private PotionStructureSanitizerGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void newlyPlacedStructureKeepsOnlyWaterAndUnrelatedItems(final GameTestHelper helper) {
        final BlockPos sourceRelative = new BlockPos(1, 1, 1);
        final BlockPos destinationRelative = new BlockPos(3, 1, 1);
        final BlockPos source = helper.absolutePos(sourceRelative);
        final BlockPos destination = helper.absolutePos(destinationRelative);
        helper.setBlock(sourceRelative, Blocks.CHEST);

        final Container sourceChest = (Container) helper.getBlockEntity(sourceRelative);
        sourceChest.setItem(0, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER));
        sourceChest.setItem(1, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING));
        sourceChest.setItem(2, PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.WATER));
        sourceChest.setItem(3, PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), Potions.WATER));
        sourceChest.setItem(4, PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), Potions.POISON));
        sourceChest.setItem(5, new ItemStack(Items.DIAMOND));
        sourceChest.setChanged();

        final StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(helper.getLevel(), source, new Vec3i(1, 1, 1), false, Blocks.STRUCTURE_VOID);
        helper.setBlock(sourceRelative, Blocks.AIR);
        template.placeInWorld(
                helper.getLevel(),
                destination,
                destination,
                new StructurePlaceSettings(),
                helper.getLevel().getRandom(),
                2);

        final Container placedChest = (Container) helper.getLevel().getBlockEntity(destination);
        helper.assertTrue(placedChest != null, "Structure placement must create the destination chest");
        helper.assertTrue(
                placedChest.getItem(0).is(Items.POTION)
                        && PotionUtils.getPotion(placedChest.getItem(0)) == Potions.WATER,
                "Plain water bottle must remain in a newly placed structure");
        helper.assertTrue(placedChest.getItem(5).is(Items.DIAMOND), "Unrelated structure loot must remain");
        for (int slot = 1; slot <= 4; slot++) {
            helper.assertTrue(placedChest.getItem(slot).isEmpty(), "Disabled potion delivery must be removed");
        }
        helper.succeed();
    }
}
