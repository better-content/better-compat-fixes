package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.BackpackArmorPolicy;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import java.util.UUID;

/** Runs with and without Curios. This test and its production armor guard have no Curios linkage. */
@PrefixGameTestTemplate(false)
public final class BackpackArmorGameTests {
    private BackpackArmorGameTests() {}

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void nativeArmorAdmissionDoesNotRequireOptionalCurios(GameTestHelper helper) {
        if (Boolean.getBoolean("better_compat_fixes.expectCuriosAbsent")) {
            helper.assertTrue(!ModList.get().isLoaded("curios"), "Absence profile must actually exclude native Curios");
        }
        Player player = new Player(helper.getLevel(), BlockPos.ZERO, 0,
                new GameProfile(UUID.randomUUID(), "BackpackArmor")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
        var zombie = EntityType.ZOMBIE.create(helper.getLevel());
        helper.assertTrue(zombie != null, "Native wearing mob must exist");
        zombie.setCanPickUpLoot(true);
        try {
            var bags = ForgeRegistries.ITEMS.getValues().stream().filter(item -> item instanceof BackpackItem).toList();
            helper.assertTrue(bags.size() == 6, "All six installed tiers must be tested even when Curios is absent");
            for (var bag : bags) {
                var stack = new ItemStack(bag);
                for (var slot : EquipmentSlot.values()) {
                    if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                        helper.assertTrue(!stack.canEquip(slot, player), "Every player armor slot rejects " + bag);
                    }
                }
                for (int slot = 5; slot <= 8; slot++) {
                    helper.assertTrue(!player.inventoryMenu.getSlot(slot).mayPlace(stack), "Native armor placement rejects " + bag);
                }
                player.getInventory().setItem(9, stack.copy());
                player.inventoryMenu.quickMoveStack(player, 9);
                helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "Native shift move does not equip " + bag);
                player.getInventory().clearContent();
                helper.assertTrue(!player.canTakeItem(stack), "Player dispenser admission rejects " + bag);
                helper.assertTrue(!new EntitySelector.MobCanWearArmorEntitySelector(stack).test(player), "Native dispenser selector rejects player");
                helper.assertTrue(stack.canEquip(EquipmentSlot.CHEST, zombie), "Nonplayer wearing remains native");
                helper.assertTrue(new EntitySelector.MobCanWearArmorEntitySelector(stack).test(zombie), "Native wearing-mob selector remains enabled");
                // This is the SlotAccess used by vanilla /item authoring, not gameplay admission.
                helper.assertTrue(player.getSlot(102).set(stack.copy()), "Operator chest authoring must remain outside admission");
                helper.assertTrue(BackpackArmorPolicy.hasArmorBackpack(player), "Misplaced armor detection remains Curios-free");
                helper.assertTrue(player.getSlot(102).set(ItemStack.EMPTY), "Legacy armor bag remains removable");
                player.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
                player.setItemSlot(EquipmentSlot.OFFHAND, stack.copy());
                player.getInventory().setItem(0, stack.copy());
                helper.assertTrue(!BackpackArmorPolicy.hasArmorBackpack(player), "Hands/hotbar carry is not misplaced armor");
                player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                player.getInventory().clearContent();
            }
            var armor = new ItemStack(Items.IRON_CHESTPLATE);
            helper.assertTrue(armor.canEquip(EquipmentSlot.CHEST, player), "Unrelated armor admission remains native");
            helper.assertTrue(player.canTakeItem(armor), "Unrelated armor dispenser admission remains native");
            helper.succeed();
        } finally {
            player.invalidateCaps();
            zombie.invalidateCaps();
        }
    }
}
