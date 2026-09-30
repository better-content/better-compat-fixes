package com.bettercontent.bettercompatfixes.compat.sleeping;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.api.event.SleepTimelapseEvent;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class SleepTimelapseEventGameTests {
    private SleepTimelapseEventGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void publishesOneCorrelatedStartAndFinish(final GameTestHelper helper) {
        final ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "sleep-episode"));
        final Recorder recorder = new Recorder();
        MinecraftForge.EVENT_BUS.register(recorder);
        try {
            SleepThreadEpisodes.start(player);
            SleepThreadEpisodes.start(player);
            SleepThreadEpisodes.finish(player);
            SleepThreadEpisodes.finish(player);

            helper.assertTrue(recorder.events.size() == 2,
                    "duplicate updates must publish one start and one finish");
            final SleepTimelapseEvent started = recorder.events.get(0);
            final SleepTimelapseEvent finished = recorder.events.get(1);
            helper.assertTrue(started.kind() == SleepTimelapseEvent.Kind.STARTED,
                    "first boundary must be STARTED");
            helper.assertTrue(finished.kind() == SleepTimelapseEvent.Kind.FINISHED,
                    "second boundary must be FINISHED");
            helper.assertTrue(started.player() == player && finished.player() == player,
                    "events must expose the authoritative server player");
            helper.assertTrue(started.episodeId().equals(finished.episodeId()),
                    "start and finish must share the persisted episode ID");
            helper.assertTrue(started.respawnDimension().equals(finished.respawnDimension())
                            && java.util.Objects.equals(started.respawnPosition(), finished.respawnPosition())
                            && started.respawnAngle() == finished.respawnAngle()
                            && started.respawnForced() == finished.respawnForced(),
                    "the respawn contract must remain stable across the timelapse");
            helper.succeed();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(recorder);
        }
    }

    public static final class Recorder {
        private final List<SleepTimelapseEvent> events = new ArrayList<>();

        @SubscribeEvent
        public void onEpisode(final SleepTimelapseEvent event) {
            events.add(event);
        }
    }
}
