package com.bettercontent.bettercompatfixes.deepvoid;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Server-selected target only; there is no client-requested locate or world mutation packet. */
public final class DeepVoidCompassNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BetterContentFixes.MOD_ID, "deep_void_compass"), () -> "1", "1"::equals, "1"::equals);
    private DeepVoidCompassNetwork() { }
    public static void register() {
        CHANNEL.registerMessage(0, Target.class, Target::encode, Target::decode, (message, context) -> {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bettercontent.bettercompatfixes.client.DeepVoidCompassTarget.accept(message.origin, message.target)));
            context.get().setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void send(ServerPlayer player, BlockPos origin, BlockPos target) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Target(origin, target));
    }
    public record Target(BlockPos origin, BlockPos target) {
        static void encode(Target value, FriendlyByteBuf buf) {
            buf.writeBlockPos(value.origin);
            buf.writeBoolean(value.target != null);
            if (value.target != null) buf.writeBlockPos(value.target);
        }
        static Target decode(FriendlyByteBuf buf) {
            return new Target(buf.readBlockPos(), buf.readBoolean() ? buf.readBlockPos() : null);
        }
    }
}
