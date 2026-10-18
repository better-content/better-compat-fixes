package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import com.bettercontent.bettercompatfixes.nativefixture.DispenserRegistryAccessor;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Observe the transformed native assignment, not a source-text substitute. */
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DispenserRegistryGameTests {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) { event.register(DispenserRegistryGameTests.class); }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void nativeRegistryAssignmentAndRegistrationRemainSynchronized(GameTestHelper helper) throws Exception {
        var registry = DispenserRegistryAccessor.better_compat_fixes$registry();
        helper.assertTrue(registry.getClass().getName().equals("java.util.Collections$SynchronizedMap"),
                "Native clinit must assign a synchronized registry; silently failed mixin is not acceptable");
        boolean existed = registry.containsKey(Items.AIR);
        var previous = registry.get(Items.AIR);
        DispenseItemBehavior sentinel = (source, stack) -> stack;
        var workers = java.util.concurrent.Executors.newFixedThreadPool(4);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int worker = 0; worker < 4; worker++) {
                final boolean directWriter = worker % 2 == 0;
                futures.add(workers.submit(() -> {
                    try { start.await(); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new RuntimeException(interrupted); }
                    for (int operation = 0; operation < 1000; operation++) {
                        if (directWriter) registry.put(Items.AIR, sentinel);
                        else DispenserBlock.registerBehavior(Items.AIR, sentinel);
                        if (registry.get(Items.AIR) != sentinel) throw new AssertionError("Concurrent native/direct write lost");
                    }
                }));
            }
            start.countDown();
            for (var future : futures) future.get(5, java.util.concurrent.TimeUnit.SECONDS);
            DispenserBlock.registerBehavior(Items.AIR, sentinel);
            helper.assertTrue(registry.get(Items.AIR) == sentinel,
                    "Concurrent direct and native registration must target the same synchronized registry");
        } finally {
            start.countDown();
            workers.shutdownNow();
            helper.assertTrue(workers.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS), "Native registry fixture workers must terminate");
            if (existed) registry.put(Items.AIR, previous); else registry.remove(Items.AIR);
        }
        helper.succeed();
    }
}
