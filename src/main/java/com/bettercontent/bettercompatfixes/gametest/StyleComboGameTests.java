package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.epicfight.StyleCatalogue;
import com.bettercontent.bettercompatfixes.compat.epicfight.StyleCombat;
import com.bettercontent.bettercompatfixes.compat.epicfight.StyleState;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.skill.BasicAttack;
import yesman.epicfight.skill.SkillDataKeys;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.ComboCounterHandleEvent.Causal;

/** Actual transformed BasicAttack counter path, with a different style from the held weapon. */
@PrefixGameTestTemplate(false)
public final class StyleComboGameTests {
    private StyleComboGameTests() {}

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void selectedStyleAdvancesAndPreservesNativeResets(GameTestHelper helper) {
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "StyleComboProbe"));
        var patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        helper.assertTrue(patch != null, "Native server player patch must be attached");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        var held = EpicFightCapabilities.getItemStackCapability(player.getMainHandItem());
        var donor = new ItemStack(Items.IRON_AXE);
        var entry = StyleCatalogue.observe(patch, donor, EpicFightCapabilities.getItemStackCapability(donor));
        helper.assertTrue(entry != null, "Axe style must be available");
        StyleState.learn(player, patch, entry);
        helper.assertTrue(StyleState.select(player, patch, entry.id()), "Style must be selectable");
        var motions = StyleCombat.combo(held, patch);
        helper.assertTrue(!motions.equals(held.getAutoAttackMotion(patch)), "Fixture must cross weapon styles");
        helper.assertTrue(motions.size() > 3, "Fixture needs multiple ordinary swings");
        helper.assertTrue(held.handleComboCounter(Causal.ANOTHER_ACTION_ANIMATION,
                patch, motions.get(0), 1) == 0, "Negative control: held weapon rejects selected swing");
        helper.assertTrue(StyleCombat.counter(held, Causal.ANOTHER_ACTION_ANIMATION,
                patch, motions.get(0), 1) == 1, "Selected validation must repair native reset");
        var container = patch.getSkill(SkillSlots.BASIC_ATTACK);
        var key = SkillDataKeys.COMBO_COUNTER.get();
        // This unjoined player deliberately has no login/network initialization.
        container.getDataManager().registerData(key);
        for (int index = 0; index < motions.size() - 2; index++) {
            int next = (index + 1) % (motions.size() - 2);
            container.getDataManager().setData(key, next);
            BasicAttack.setComboCounterWithEvent(Causal.ANOTHER_ACTION_ANIMATION,
                    patch, container, motions.get(index), next);
            helper.assertTrue(container.getDataManager().getDataValue(key) == next,
                    "Selected ordinary swing must preserve next combo index " + next);
        }
        for (int index = motions.size() - 2; index < motions.size(); index++) {
            BasicAttack.setComboCounterWithEvent(Causal.ANOTHER_ACTION_ANIMATION,
                    patch, container, motions.get(index), 1);
            helper.assertTrue(container.getDataManager().getDataValue(key) == 0,
                    "Dash/air attack must reset combo");
        }
        BasicAttack.setComboCounterWithEvent(Causal.TIME_EXPIRED, patch, container, null, 1);
        helper.assertTrue(container.getDataManager().getDataValue(key) == 0, "Timeout must reset combo");
        var nativeMotion = held.getAutoAttackMotion(patch).get(0);
        helper.assertTrue(StyleCombat.counter(held, Causal.ANOTHER_ACTION_ANIMATION, patch, nativeMotion, 1)
                == held.handleComboCounter(Causal.ANOTHER_ACTION_ANIMATION, patch, nativeMotion, 1),
                "Unrelated native action must retain weapon handling");
        StyleState.select(player, patch, "");
        helper.assertTrue(StyleCombat.counter(held, Causal.ANOTHER_ACTION_ANIMATION, patch, motions.get(0), 1)
                == held.handleComboCounter(Causal.ANOTHER_ACTION_ANIMATION, patch, motions.get(0), 1),
                "Default selection must retain native handling");
        helper.succeed();
    }
}
