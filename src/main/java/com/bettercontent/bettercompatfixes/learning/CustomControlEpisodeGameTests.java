package com.bettercontent.bettercompatfixes.learning;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.api.event.CustomControlEpisodeEvent;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class CustomControlEpisodeGameTests {
    private CustomControlEpisodeGameTests() {
    }

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty")
    public static void publishesOneCorrelatedBoundaryPerAcceptedTransition(final GameTestHelper helper) {
        final ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "control-episode"));
        final Recorder recorder = new Recorder();
        MinecraftForge.EVENT_BUS.register(recorder);
        try {
            CustomControlEpisodes.record(player, "parcool:dodge");
            CustomControlEpisodes.record(player, "parcool:dodge");
            CustomControlEpisodes.record(player, "parcool:vault");

            helper.assertTrue(recorder.events.size() == 2,
                    "first, repeated, and distinct controls must publish exactly two boundaries");
            final CustomControlEpisodeEvent first = recorder.events.get(0);
            final CustomControlEpisodeEvent second = recorder.events.get(1);
            helper.assertTrue(first.kind() == CustomControlEpisodeEvent.Kind.FIRST_ACCEPTED,
                    "the first boundary must identify the accepted first action");
            helper.assertTrue(second.kind() == CustomControlEpisodeEvent.Kind.DISTINCT_SECOND,
                    "the second distinct action must close the episode");
            helper.assertTrue(first.player() == player && second.player() == player,
                    "events must expose the authoritative server player");
            helper.assertTrue("parcool:dodge".equals(first.acceptedActionId())
                            && "parcool:vault".equals(second.acceptedActionId())
                            && "parcool:dodge".equals(second.firstActionId()),
                    "events must retain the accepted action sequence");
            helper.assertTrue(first.episodeId().equals(second.episodeId()),
                    "both boundaries must share the provider-generated episode ID");
            helper.assertTrue(!player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG)
                            .contains(CustomControlEpisodes.PERSISTED_KEY),
                    "a completed episode must not remain persisted");
            helper.succeed();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(recorder);
        }
    }

    public static final class Recorder {
        private final List<CustomControlEpisodeEvent> events = new ArrayList<>();

        @SubscribeEvent
        public void onEpisode(final CustomControlEpisodeEvent event) {
            events.add(event);
        }
    }
}
