package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.BackpackEquipmentPolicy;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioEquipEvent;
import top.theillusivec4.curios.common.inventory.DynamicStackHandler;
import java.util.UUID;

/** Real transformed native admissions. No GUI input, storage protocol or fixture commands. */
@PrefixGameTestTemplate(false)
public final class BackpackEquipmentGameTests {
    private BackpackEquipmentGameTests() {}

    public static final class CompetingListener {
        private Event.Result result = Event.Result.ALLOW;
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void equip(CurioEquipEvent event) { event.setResult(result); }
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void emptyBottlePolicyLeavesProviderAdmissionIntact(GameTestHelper helper) {
        Player player = new Player(helper.getLevel(), BlockPos.ZERO, 0,
                new GameProfile(UUID.randomUUID(), "EmptyBottleAdmission")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
        try {
            for (int count : new int[]{1, 64}) {
                var stack = new ItemStack(Items.GLASS_BOTTLE, count);
                var context = new SlotContext("empty_bottle", player, 0, false, true);
                var event = new CurioEquipEvent(stack, context);
                MinecraftForge.EVENT_BUS.post(event);
                helper.assertTrue(event.getResult() == Event.Result.DEFAULT,
                        "Valid glass bottles must reach Drinking Water's validator, not be denied or force-allowed");
            }
            for (boolean cosmetic : new boolean[]{false, true}) {
                for (int index = 0; index < 2; index++) {
                    for (var item : new net.minecraft.world.item.Item[]{Items.GLASS_BOTTLE, Items.POTION, Items.STONE}) {
                        var context = new SlotContext("empty_bottle", player, index, cosmetic, true);
                        var event = new CurioEquipEvent(new ItemStack(item), context);
                        MinecraftForge.EVENT_BUS.post(event);
                        boolean valid = item == Items.GLASS_BOTTLE && index == 0 && !cosmetic;
                        helper.assertTrue(event.getResult() == (valid ? Event.Result.DEFAULT : Event.Result.DENY),
                                "Only glass bottles in functional empty_bottle[0] may reach native validation");
                    }
                }
            }
            for (String slot : new String[]{"water", "better_backpack", "unknown"}) {
                var event = new CurioEquipEvent(new ItemStack(Items.GLASS_BOTTLE),
                        new SlotContext(slot, player, 0, false, true));
                MinecraftForge.EVENT_BUS.post(event);
                helper.assertTrue(event.getResult() == Event.Result.DENY,
                        "Empty-bottle exception must not leak into " + slot);
            }
            helper.succeed();
        } finally {
            player.invalidateCaps();
        }
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void drinkingProviderOwnsExpandedVesselAdmission(GameTestHelper helper) {
        Player player = new Player(helper.getLevel(), BlockPos.ZERO, 0,
                new GameProfile(UUID.randomUUID(), "WaterProviderAdmission")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
        var waterKey = new net.minecraft.resources.ResourceLocation("better_drinking_water", "water_bottle");
        var emptyKey = new net.minecraft.resources.ResourceLocation("better_drinking_water", "empty_bottle");
        var oldWater = CuriosApi.getCurioPredicate(waterKey);
        var oldEmpty = CuriosApi.getCurioPredicate(emptyKey);
        var active = new java.util.concurrent.atomic.AtomicBoolean(true);
        try {
            // Native registration is putIfAbsent, not replacement. Register reversible fixture
            // callbacks only in the provider-absent source profile, never overwrite a real provider.
            helper.assertTrue(oldWater.isEmpty() && oldEmpty.isEmpty(), "This fixture expects Drinking Water absent");
            CuriosApi.registerCurioPredicate(waterKey, result -> active.get() ? result.stack().is(Items.WATER_BUCKET)
                    : result.stack().is(Items.POTION) && net.minecraft.world.item.alchemy.PotionUtils.getPotion(result.stack())
                    == net.minecraft.world.item.alchemy.Potions.WATER);
            CuriosApi.registerCurioPredicate(emptyKey, result -> active.get() ? result.stack().is(Items.BUCKET)
                    : result.stack().is(Items.GLASS_BOTTLE));
            for (String slot : new String[]{"water", "empty_bottle"}) {
                var approved = slot.equals("water") ? Items.WATER_BUCKET : Items.BUCKET;
                for (int index : new int[]{0, 1}) {
                    for (boolean cosmetic : new boolean[]{false, true}) {
                        var event = new CurioEquipEvent(new ItemStack(approved),
                                new SlotContext(slot, player, index, cosmetic, true));
                        MinecraftForge.EVENT_BUS.post(event);
                        helper.assertTrue(event.getResult() == (index == 0 && !cosmetic
                                        ? Event.Result.DEFAULT : Event.Result.DENY),
                                "Provider-approved vessels must reach native admission only in functional slot zero");
                    }
                }
                var rejected = new CurioEquipEvent(new ItemStack(Items.GLASS_BOTTLE),
                        new SlotContext(slot, player, 0, false, true));
                MinecraftForge.EVENT_BUS.post(rejected);
                helper.assertTrue(rejected.getResult() == Event.Result.DENY,
                        "Compat must not widen the provider's actual predicate");
            }
            helper.succeed();
        } finally {
            active.set(false); // Leave both callbacks with exactly the provider-absent fallback behavior.
            player.invalidateCaps();
        }
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void allNativeTiersRemainExclusiveDespiteCompetingListeners(GameTestHelper helper) {
        Player player = new Player(helper.getLevel(), BlockPos.ZERO, 0,
                new GameProfile(UUID.randomUUID(), "BackpackAdmission")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
        var zombie = EntityType.ZOMBIE.create(helper.getLevel());
        helper.assertTrue(zombie != null, "Native wearing-mob fixture must exist");
        zombie.setCanPickUpLoot(true); // Native dispenser eligibility requires an enabled wearing mob.
        var competitor = new CompetingListener();
        MinecraftForge.EVENT_BUS.register(competitor);
        try {
            var bags = ForgeRegistries.ITEMS.getValues().stream().filter(item -> item instanceof BackpackItem).toList();
            helper.assertTrue(bags.size() == 6, "Pinned native fixture must contain all six installed backpack tiers");
            for (var bag : bags) {
                var stack = new ItemStack(bag);
                for (String slot : new String[]{"better_backpack", "back", "body", "curio", "charm", "unknown"}) {
                    for (boolean cosmetic : new boolean[]{false, true}) {
                        for (int index = 0; index < 2; index++) {
                            int testedIndex = index;
                            var handler = new DynamicStackHandler(2,
                                    i -> new SlotContext(slot, player, i, cosmetic, true));
                            var context = new SlotContext(slot, player, index, cosmetic, true);
                            boolean allowed = BackpackEquipmentPolicy.exclusiveSlot(slot, index, cosmetic);
                            for (var result : new Event.Result[]{Event.Result.ALLOW, Event.Result.DENY}) {
                                competitor.result = result;
                                helper.assertTrue(handler.isItemValid(testedIndex, stack) == allowed,
                                        "Final handler admission differs for " + bag + "/" + context + "/" + result);
                                helper.assertTrue(CuriosApi.isStackValid(context, stack) == allowed,
                                        "Final API admission differs for " + context);
                                helper.assertTrue(handler.insertItem(index, stack.copy(), true).isEmpty() == allowed,
                                        "Simulated native insertion must use final admission");
                                helper.assertTrue(handler.insertItem(index, stack.copy(), false).isEmpty() == allowed,
                                        "Actual native insertion must use final admission");
                                if (allowed) {
                                    helper.assertTrue(!handler.extractItem(index, 1, false).isEmpty(), "Valid bag remains removable");
                                }
                            }
                            // Saved-state/command authoring deliberately bypasses admission, and removal still works.
                            handler.setStackInSlot(index, stack.copy());
                            helper.assertTrue(!handler.extractItem(index, 1, false).isEmpty(), "Legacy bag remains removable");
                        }
                    }
                }
                for (var slot : EquipmentSlot.values()) {
                    if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                        helper.assertTrue(!stack.canEquip(slot, player), "Player armor admission rejects every tier");
                    }
                }
                for (int slot = 5; slot <= 8; slot++) {
                    helper.assertTrue(!player.inventoryMenu.getSlot(slot).mayPlace(stack), "Native armor slot denies placement");
                }
                player.getInventory().setItem(9, stack.copy());
                player.inventoryMenu.quickMoveStack(player, 9);
                helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "Shift move must not equip bag");
                player.getInventory().clearContent();
                helper.assertTrue(!player.canTakeItem(stack), "Dispenser eligibility rejects player backpack");
                helper.assertTrue(!new EntitySelector.MobCanWearArmorEntitySelector(stack).test(player), "Native dispenser selector denies player");
                helper.assertTrue(stack.canEquip(EquipmentSlot.CHEST, zombie), "Mob backpack wearing preserved");
                helper.assertTrue(new EntitySelector.MobCanWearArmorEntitySelector(stack).test(zombie), "Mob dispenser eligibility preserved");
                player.setItemSlot(EquipmentSlot.CHEST, stack.copy());
                helper.assertTrue(BackpackEquipmentPolicy.hasMisplacedBackpack(player), "Login warning detects old armor bag");
                helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).getItem() == bag, "No automatic migration/deletion");
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                helper.assertTrue(!BackpackEquipmentPolicy.hasMisplacedBackpack(player), "Carry/hotbar/hands do not warn");
            }
            competitor.result = Event.Result.ALLOW;
            for (boolean cosmetic : new boolean[]{false, true}) {
                var reserved = new DynamicStackHandler(2,
                        i -> new SlotContext("better_backpack", player, i, cosmetic, true));
                for (int index = 0; index < 2; index++) {
                    var nonBackpack = new ItemStack(Items.STICK);
                    var context = new SlotContext("better_backpack", player, index, cosmetic, true);
                    helper.assertTrue(!reserved.isItemValid(index, nonBackpack), "Competing ALLOW cannot admit a nonbag to reserved slot");
                    helper.assertTrue(!reserved.isItemValid(index, ItemStack.EMPTY), "Empty validation must not admit a reserved-slot item");
                    helper.assertTrue(!CuriosApi.isStackValid(context, nonBackpack), "Reserved-slot API rejects nonbackpacks");
                    helper.assertTrue(!reserved.insertItem(index, nonBackpack, true).isEmpty(), "Reserved-slot simulated insertion returns nonbag");
                }
            }
            var unrelated = new DynamicStackHandler(1, i -> new SlotContext("back", player, i, false, true));
            helper.assertTrue(unrelated.isItemValid(0, new ItemStack(Items.STICK)), "Unrelated event result remains native");
            helper.assertTrue(new ItemStack(Items.IRON_CHESTPLATE).canEquip(EquipmentSlot.CHEST, player), "Unrelated armor remains admissible");
            var mobHandler = new DynamicStackHandler(1, i -> new SlotContext("back", zombie, i, false, true));
            helper.assertTrue(mobHandler.isItemValid(0, new ItemStack(bags.get(0))), "Nonplayer Curios results remain native");
            helper.succeed();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(competitor);
            player.invalidateCaps();
            if (zombie != null) zombie.invalidateCaps();
        }
    }
}
