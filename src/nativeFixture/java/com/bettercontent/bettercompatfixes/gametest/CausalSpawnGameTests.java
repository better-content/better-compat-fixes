package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.api.CausalMobSpawns;
import com.bettercontent.bettercompatfixes.api.event.MobOffspringAdmissionEvent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Nonshipping native plumbing fixture; real monster behavior lives in the Director world fixture. */
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CausalSpawnGameTests {
    @SubscribeEvent public static void register(RegisterGameTestsEvent event) { event.register(CausalSpawnGameTests.class); }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void actualInsertionResultAndIndependentVetoCompleteExactlyOnce(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Zombie parent = EntityType.ZOMBIE.create(level), child = EntityType.ZOMBIE.create(level);
        Probe probe = new Probe(); probe.veto = child; MinecraftForge.EVENT_BUS.register(probe);
        try {
            helper.assertTrue(!CausalMobSpawns.as(parent, () -> level.addFreshEntity(child)), "independent join veto must return false");
            helper.assertTrue(probe.events.size() == 1 && probe.events.get(0).parent() == parent, "real ServerLevel insertion must report its actual causal parent");
            helper.assertTrue(probe.completions.equals(List.of(false)), "failed insertion must complete once with false");
            helper.assertTrue(level.getEntity(child.getUUID()) == null, "vetoed child must not enter the world");
            helper.assertTrue(CausalMobSpawns.actor() == null, "insertion must restore its caller's scope");
            probe.veto = null;
            Zombie accepted = EntityType.ZOMBIE.create(level);
            helper.assertTrue(CausalMobSpawns.as(parent, () -> level.addFreshEntity(accepted)), "nonvetoed native insertion must succeed");
            helper.assertTrue(probe.completions.equals(List.of(false, true)), "completion must use actual insertion success");
            accepted.discard(); helper.succeed();
        } finally { MinecraftForge.EVENT_BUS.unregister(probe); }
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void unownedVictimsChildIsNotAttributedToEnclosingAttacker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Zombie attacker = EntityType.ZOMBIE.create(level);
        Victim victim = new Victim(level);
        victim.setHealth(1);
        Probe probe = new Probe(); MinecraftForge.EVENT_BUS.register(probe);
        try {
            CausalMobSpawns.runAs(attacker, () -> victim.hurt(level.damageSources().mobAttack(attacker), 100));
            helper.assertTrue(probe.events.size() == 1 && probe.events.get(0).parent() == victim,
                    "native damage scope must identify the unowned victim, never its enclosing attacker");
            helper.assertTrue(probe.completions.equals(List.of(true)), "victim's actual child insertion must complete");
            helper.assertTrue(CausalMobSpawns.actor() == null, "nested death/damage must restore context");
            probe.events.forEach(e -> e.child().discard()); helper.succeed();
        } finally { MinecraftForge.EVENT_BUS.unregister(probe); }
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void vanillaRemovalScopesEachIndependentSlimeParent(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Slime first = EntityType.SLIME.create(level), second = EntityType.SLIME.create(level);
        first.setSize(2, true); second.setSize(2, true);
        first.moveTo(helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)).getCenter());
        second.moveTo(helper.absolutePos(new net.minecraft.core.BlockPos(3, 2, 3)).getCenter());
        level.addFreshEntity(first); level.addFreshEntity(second);
        Probe probe = new Probe(); MinecraftForge.EVENT_BUS.register(probe);
        try {
            first.setHealth(0); first.remove(Entity.RemovalReason.KILLED);
            second.setHealth(0); second.remove(Entity.RemovalReason.KILLED);
            long a = probe.events.stream().filter(e -> e.parent() == first).count();
            long b = probe.events.stream().filter(e -> e.parent() == second).count();
            helper.assertTrue(a >= 2 && a <= 4 && b >= 2 && b <= 4, "each real slime removal must report its own two-to-four native children");
            helper.assertTrue(probe.completions.stream().allMatch(Boolean::booleanValue), "native split children must really insert");
            helper.assertTrue(probe.events.stream().allMatch(e -> !e.child().getTags().contains("better_cave_encounters_spawned")),
                    "Compat causal observation alone must not mark ordinary children");
            helper.assertTrue(CausalMobSpawns.actor() == null, "slime removal must restore context");
            probe.events.forEach(e -> e.child().discard()); helper.succeed();
        } finally { MinecraftForge.EVENT_BUS.unregister(probe); }
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void migratedLegacyVetoWaivesOnlyItself(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var id = new net.minecraft.resources.ResourceLocation("tconstruct:ender_slime");
        helper.assertTrue(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.containsKey(id), "default native fixture must supply its declared TConstruct pin");
        Mob mob = (Mob) net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(id).create(level);
        var p = helper.absolutePos(new net.minecraft.core.BlockPos(1, 0, 1));
        var underground = new net.minecraft.core.BlockPos(p.getX(), -40, p.getZ());
        var canopy = new net.minecraft.core.BlockPos(p.getX(), 80, p.getZ());
        level.setBlock(canopy, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        mob.moveTo(underground.getCenter());
        var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                new net.minecraft.resources.ResourceLocation(BetterContentFixes.MOD_ID, "legacy_underground_ambient_denied"));
        helper.assertTrue(mob.getType().is(tag), "native fixture must load the exact pack-owned legacy tag");
        Probe probe = new Probe(); MinecraftForge.EVENT_BUS.register(probe);
        try {
            var denied = position(mob, level, net.minecraft.world.entity.MobSpawnType.NATURAL);
            com.bettercontent.bettercompatfixes.compat.AmbientSurfaceSpawnControl.onPositionCheck(denied);
            helper.assertTrue(denied.getResult() == net.minecraftforge.eventbus.api.Event.Result.DENY, "no geological answer must retain the old veto");
            probe.exempt = mob.getType(); probe.exemptPosition = mob.blockPosition();
            var waived = position(mob, level, net.minecraft.world.entity.MobSpawnType.NATURAL);
            com.bettercontent.bettercompatfixes.compat.AmbientSurfaceSpawnControl.onPositionCheck(waived);
            helper.assertTrue(waived.getResult() == net.minecraftforge.eventbus.api.Event.Result.DEFAULT, "query may waive legacy veto but must never force overall ALLOW");
            var independent = position(mob, level, net.minecraft.world.entity.MobSpawnType.NATURAL);
            independent.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            com.bettercontent.bettercompatfixes.compat.AmbientSurfaceSpawnControl.onPositionCheck(independent);
            helper.assertTrue(independent.getResult() == net.minecraftforge.eventbus.api.Event.Result.DENY, "geological eligibility must preserve another denial");
            var generation = position(mob, level, net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION);
            com.bettercontent.bettercompatfixes.compat.AmbientSurfaceSpawnControl.onPositionCheck(generation);
            helper.assertTrue(generation.getResult() == net.minecraftforge.eventbus.api.Event.Result.DEFAULT, "migrated rule remains NATURAL-only");
            for (int y = 50; y < level.getMaxBuildHeight(); y++)
                level.setBlock(new net.minecraft.core.BlockPos(p.getX(), y, p.getZ()), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(new net.minecraft.core.BlockPos(p.getX(), 49, p.getZ()), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
            mob.moveTo(p.getX() + .5, 50, p.getZ() + .5); probe.exemptPosition = mob.blockPosition();
            var surface = position(mob, level, net.minecraft.world.entity.MobSpawnType.NATURAL);
            com.bettercontent.bettercompatfixes.compat.AmbientSurfaceSpawnControl.onPositionCheck(surface);
            helper.assertTrue(surface.getResult() == net.minecraftforge.eventbus.api.Event.Result.DENY, "positive query must not waive surface reserve");
            helper.succeed();
        } finally { MinecraftForge.EVENT_BUS.unregister(probe); }
    }
    private static net.minecraftforge.event.entity.living.MobSpawnEvent.PositionCheck position(Mob mob, ServerLevel level,
                                                                                              net.minecraft.world.entity.MobSpawnType reason) {
        return new net.minecraftforge.event.entity.living.MobSpawnEvent.PositionCheck(mob, level, reason, null);
    }

    public static final class Probe {
        final List<MobOffspringAdmissionEvent> events = new ArrayList<>();
        final List<Boolean> completions = new ArrayList<>();
        Mob veto;
        EntityType<?> exempt;
        net.minecraft.core.BlockPos exemptPosition;
        @SubscribeEvent public void geology(com.bettercontent.bettercompatfixes.api.event.GeologicalAmbientEligibilityEvent event) {
            if (event.type() == exempt && event.position().equals(exemptPosition))
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
        }
        @SubscribeEvent public void offspring(MobOffspringAdmissionEvent event) { events.add(event); event.onCompletion(completions::add); }
        @SubscribeEvent(priority = EventPriority.LOWEST) public void joined(EntityJoinLevelEvent event) {
            if (event.getEntity() == veto) event.setCanceled(true);
        }
    }

    private static final class Victim extends Zombie {
        Victim(ServerLevel level) { super(EntityType.ZOMBIE, level); }
        @Override public void die(DamageSource damage) {
            // Deliberately before super.die: the native hurt wrapper must already have
            // masked the enclosing attacker even when a subclass overrides death.
            level().addFreshEntity(EntityType.ZOMBIE.create(level()));
            super.die(damage);
        }
    }
}
