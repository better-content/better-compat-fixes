package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.mixin.forgeendertech.WorldDataAccessor;
import com.endertech.minecraft.forge.events.ChunkFullyLoadedEvent;
import com.endertech.minecraft.forge.world.GameWorld;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class ForgeEndertechChunkReadyGameTests {
    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "daylight_platform", timeoutTicks = 40)
    public static void nativeNotificationRetainsDeferredEntriesAndLoadedCallbacksAndTimeout(GameTestHelper helper) {
        var level = helper.getLevel();
        var pending = ((WorldDataAccessor) GameWorld.getData(level)).better_compat_fixes$getFreshlyLoadedChunks();
        var loadedPos = new ChunkPos(helper.absolutePos(new BlockPos(2, 1, 2)));
        var loaded = level.getChunkSource().getChunkNow(loadedPos.x, loadedPos.z);
        var missing = new ChunkPos(1_250_001, -1_250_001);
        helper.assertTrue(loaded != null, "native loaded notification fixture must be resident FULL");
        helper.assertTrue(level.getChunkSource().getChunkNow(missing.x, missing.z) == null, "deferred column must begin absent");
        var delivered = new AtomicInteger();
        Consumer<ChunkFullyLoadedEvent> listener = event -> {
            if (event.getLevel() == level && event.getChunk() == loaded) delivered.incrementAndGet();
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ChunkFullyLoadedEvent.class, listener);
        try {
            pending.put(loadedPos, level.getGameTime() - 1);
            pending.put(missing, level.getGameTime() - 1);
            var tick = new TickEvent.LevelTickEvent(LogicalSide.SERVER, TickEvent.Phase.START, level, () -> true);
            GameWorld.WorldData.onLevelTick(tick);
            helper.assertTrue(delivered.get() == 1 && !pending.containsKey(loadedPos), "preserve exactly one actual native FULL-chunk callback");
            helper.assertTrue(pending.containsKey(missing), "unavailable entry must retain its native retry state");
            helper.assertTrue(level.getChunkSource().getChunkNow(missing.x, missing.z) == null, "deferred notification must not generate terrain");
            pending.put(missing, level.getGameTime() - 100);
            GameWorld.WorldData.onLevelTick(tick);
            helper.assertTrue(!pending.containsKey(missing), "preserve the native 100-tick expiry rather than inventing a scheduler");
            helper.assertTrue(delivered.get() == 1, "native callback must not be duplicated");
            helper.assertTrue(level.getChunkSource().getChunkNow(missing.x, missing.z) == null, "expiry must not generate terrain");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
            pending.remove(missing);
            pending.remove(loadedPos);
        }
        helper.succeed();
    }
}
