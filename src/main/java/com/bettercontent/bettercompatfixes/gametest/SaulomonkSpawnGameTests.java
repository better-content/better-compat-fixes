package com.bettercontent.bettercompatfixes.gametest;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.SaulomonkSpawnDistance;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import reascer.wom.world.entity.WOMEntities;
import reascer.wom.world.entity.mob.Saulomonk;

/** Calls the transformed pinned native predicate with the natural-spawn reason. */
@PrefixGameTestTemplate(false)
public final class SaulomonkSpawnGameTests {
    private SaulomonkSpawnGameTests() {}

    @GameTest(templateNamespace = BetterContentFixes.MOD_ID, template = "empty", batch = "saulomonk_spawn")
    public static void selectedSpawnReplacesWorldOriginWithoutReplacingNativeGuards(GameTestHelper helper) {
        var level = helper.getLevel();
        var candidate = helper.absolutePos(new BlockPos(2, 2, 2));
        var first = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "SaulomonkNear"));
        var second = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "SaulomonkFar"));
        first.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), first);
        second.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), second);
        first.setPos(candidate.getX() + 30, candidate.getY(), candidate.getZ());
        second.setPos(candidate.getX() + 60, candidate.getY(), candidate.getZ());
        var previousPlayers = new java.util.ArrayList<>(level.players());
        var oldDifficulty = level.getDifficulty();
        var oldGround = level.getBlockState(candidate.below());
        // No sockets/login or player-data files: expose these probes only to the synchronous selector.
        level.players().clear();
        level.players().add(first);
        level.players().add(second);
        try {
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            level.setBlock(candidate.below(), Blocks.SAND.defaultBlockState(), 3);
            setPoint(second, candidate.getX() + 4000, candidate.getY(), candidate.getZ());
            assertSpawn(helper, candidate, false, "Missing nearest anchor cannot borrow farther player's point");
            for (int distance : new int[] {1999, 2000, 2001}) {
                setPoint(first, candidate.getX() + distance, candidate.getY(), candidate.getZ());
                assertSpawn(helper, candidate, distance > 2000, "Strict horizontal boundary " + distance);
            }
            // Altitude and world origin are not exploration anchors.
            setPoint(first, candidate.getX(), candidate.getY() + 20000, candidate.getZ());
            assertSpawn(helper, candidate, false, "Altitude cannot unlock the encounter");
            setPoint(first, candidate.getX() + 2001, candidate.getY(), candidate.getZ());
            first.getPersistentData().putString("better_spawns:respawn_dim", "minecraft:the_nether");
            assertSpawn(helper, candidate, false, "Cross-dimension point must not be compared");
            setPoint(first, candidate.getX(), candidate.getY(), candidate.getZ());
            first.setGameMode(GameType.SPECTATOR);
            assertSpawn(helper, candidate, true, "Spectators do not select the anchor");
            first.setGameMode(GameType.SURVIVAL);
            first.setPos(candidate.getX() + 100, candidate.getY(), candidate.getZ());
            assertSpawn(helper, candidate, true, "Moving players changes which permanent point is selected");
            setPoint(second, candidate.getX() + 2001, candidate.getY(), candidate.getZ());
            level.setBlock(candidate.below(), Blocks.STONE.defaultBlockState(), 3);
            assertSpawn(helper, candidate, false, "Native sand guard must survive");
            level.setBlock(candidate.below(), Blocks.SAND.defaultBlockState(), 3);
            level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
            assertSpawn(helper, candidate, false, "Native difficulty guard must survive");
            helper.succeed();
        } finally {
            level.players().clear();
            level.players().addAll(previousPlayers);
            level.setBlock(candidate.below(), oldGround, 3);
            level.getServer().setDifficulty(oldDifficulty, true);
        }
    }

    private static void setPoint(ServerPlayer player, int x, int y, int z) {
        var data = player.getPersistentData();
        data.putString("better_spawns:respawn_dim", player.level().dimension().location().toString());
        data.putInt("better_spawns:respawn_x", x);
        data.putInt("better_spawns:respawn_y", y);
        data.putInt("better_spawns:respawn_z", z);
    }

    private static void assertSpawn(GameTestHelper helper, BlockPos candidate, boolean expected, String message) {
        boolean actual = Saulomonk.checkSaulomonkSpawnRules(WOMEntities.SAULOMONK.get(), helper.getLevel(),
                MobSpawnType.NATURAL, candidate, RandomSource.create(1234));
        helper.assertTrue(actual == expected, message + "; distance="
                + SaulomonkSpawnDistance.fromNearestPlayer(helper.getLevel(), candidate));
    }
}
