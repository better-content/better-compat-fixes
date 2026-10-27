package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.TconCombatFirstEvents;
import com.bettercontent.bettercompatfixes.compat.TconCombatFirstPolicy;
import com.bettercontent.bettercompatfixes.compat.tconcombat.TconCombatFirstIntegration;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.tools.definition.ToolDefinitionData;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/** Nonshipping server behavior only. This is NOT client input/animation acceptance. */
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class TconCombatFirstGameTests {
    private TconCombatFirstGameTests() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (Boolean.getBoolean("better_compat_fixes.tconCombatFirstFixture")
                && ModList.get().isLoaded("tconstruct") && ModList.get().isLoaded("epicfight"))
            event.register(TconCombatFirstGameTests.class);
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void loadedCapabilitiesAndDefinitionsDrivePolicy(GameTestHelper helper) {
        int protectedCount = 0;
        int miningCount = 0;
        for (var item : ForgeRegistries.ITEMS.getValues()) {
            if (!(item instanceof IModifiable tool) || !tool.getToolDefinition().isDataLoaded()) continue;
            var stack = ToolBuildHandler.buildItemRandomMaterials(tool, RandomSource.create(481));
            var capability = EpicFightCapabilities.getItemStackCapability(stack);
            if (capability != null) capability = capability.getResult(stack);
            boolean mining = tool.getToolDefinition().getData().getHooks().hasHook(ToolHooks.IS_EFFECTIVE);
            boolean expected = !mining && capability != null && !capability.isEmpty();
            helper.assertTrue(TconCombatFirstIntegration.deniesBlockAttack(stack) == expected,
                    "Wrong loaded eligibility: " + ForgeRegistries.ITEMS.getKey(item));
            if (expected) {
                protectedCount++;
                // Neither raw material/broken metadata nor definition mining speed changes the category.
                stack.getOrCreateTag().putBoolean("tic_broken", true);
                helper.assertTrue(TconCombatFirstIntegration.deniesBlockAttack(stack), "Broken status changed category");
            }
            if (mining) miningCount++;
        }
        helper.assertTrue(protectedCount > 0 && miningCount > 0, "Fixture did not exercise both categories");
        for (String name : List.of("battlesign", "javelin", "longbow", "plate_shield"))
            helper.assertTrue(TconCombatFirstIntegration.deniesBlockAttack(tool("tconstruct:" + name)),
                    "Controlled melee/ranged/shield fixture did not qualify: " + name);
        for (String name : List.of("pickaxe", "mattock", "sword", "dagger", "scythe", "melting_pan"))
            helper.assertTrue(!TconCombatFirstIntegration.deniesBlockAttack(tool("tconstruct:" + name)), name);
        helper.assertTrue(!TconCombatFirstIntegration.deniesBlockAttack(ItemStack.EMPTY), "Empty hand denied");
        helper.assertTrue(!TconCombatFirstIntegration.deniesBlockAttack(new ItemStack(Items.DIAMOND_SWORD)),
                "Non-TCon weapon denied");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void reloadAndMainHandChangesAreImmediate(GameTestHelper helper) {
        var stack = tool("tconstruct:battlesign");
        helper.assertTrue(TconCombatFirstIntegration.deniesBlockAttack(stack), "Battlesign must qualify in fixture");
        var definition = ((IModifiable) stack.getItem()).getToolDefinition();
        var original = definition.getData();
        var mining = ((IModifiable) tool("tconstruct:pickaxe").getItem()).getToolDefinition().getData();
        // Exercise the real data setter used by reloads, synchronously, restoring before yielding a tick.
        try {
            definition.setData(ToolDefinitionData.EMPTY);
            helper.assertTrue(!TconCombatFirstIntegration.deniesBlockAttack(stack), "Unloaded definition denied");
            definition.setData(mining);
            helper.assertTrue(!TconCombatFirstIntegration.deniesBlockAttack(stack), "New effectiveness ignored");
            definition.setData(original);
            helper.assertTrue(TconCombatFirstIntegration.deniesBlockAttack(stack), "Reload stayed stale");
        } finally {
            definition.setData(original);
        }
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "tcon_swap"));
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool("tconstruct:pickaxe"));
        helper.assertTrue(!TconCombatFirstPolicy.deniesBlockAttack(player), "Offhand suppressed pickaxe");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.assertTrue(TconCombatFirstPolicy.deniesBlockAttack(player), "Main-hand swap stayed stale");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void eventsAndNativeRemovalDenyEveryGameModeButAllowAbort(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "tcon_block"));
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setItemInHand(InteractionHand.MAIN_HAND, tool("tconstruct:battlesign"));
        for (var mode : GameType.values()) {
            player.setGameMode(mode);
            for (var action : PlayerInteractEvent.LeftClickBlock.Action.values()) {
                var event = new PlayerInteractEvent.LeftClickBlock(player, pos, Direction.UP, action);
                MinecraftForge.EVENT_BUS.post(event);
                helper.assertTrue(event.isCanceled() == (action != PlayerInteractEvent.LeftClickBlock.Action.ABORT),
                        "Wrong event handling: " + mode + "/" + action);
                if (action != PlayerInteractEvent.LeftClickBlock.Action.ABORT)
                    helper.assertTrue(event.getUseBlock() == Event.Result.DENY && event.getUseItem() == Event.Result.DENY,
                            "Block/item callbacks were not denied");
            }
            helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            var event = new BlockEvent.BreakEvent(helper.getLevel(), pos, Blocks.STONE.defaultBlockState(), player);
            MinecraftForge.EVENT_BUS.post(event);
            helper.assertTrue(event.isCanceled(), "Break backstop missing in " + mode);
            player.gameMode.destroyBlock(pos);
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.STONE), "Native removal in " + mode);
        }
        // Direct negative guard: the new listeners must not change unrelated tool events.
        player.setItemInHand(InteractionHand.MAIN_HAND, tool("tconstruct:pickaxe"));
        var normal = new PlayerInteractEvent.LeftClickBlock(player, pos, Direction.UP,
                PlayerInteractEvent.LeftClickBlock.Action.START);
        TconCombatFirstEvents.onLeftClickBlock(normal);
        helper.assertTrue(!normal.isCanceled(), "Pickaxe event changed");
        helper.succeed();
    }

    private static ItemStack tool(String id) {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (!(item instanceof IModifiable tool)) throw new AssertionError("Missing fixture tool " + id);
        return ToolBuildHandler.buildItemRandomMaterials(tool, RandomSource.create(481));
    }
}
