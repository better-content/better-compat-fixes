package com.bettercontent.bettercontentfixes.compat.epicfight;

import com.bettercontent.bettercontentfixes.BetterContentFixes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public final class StyleNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BetterContentFixes.MOD_ID, "fighting_styles"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public record Row(String id, String name, List<ResourceLocation> sources, boolean learned) {}
    public record Snapshot(List<Row> rows, String selected) {}
    public record Action(String selected) {}

    private StyleNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(Snapshot.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StyleNetwork::encodeSnapshot).decoder(StyleNetwork::decodeSnapshot)
                .consumerMainThread((snapshot, context) -> {
                    context.get().enqueueWork(() -> StyleClientState.receive(snapshot));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(Action.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder((action, buffer) -> buffer.writeUtf(action.selected(), 64))
                .decoder(buffer -> new Action(buffer.readUtf(64)))
                .consumerMainThread((action, context) -> {
                    ServerPlayer player = context.get().getSender();
                    context.get().enqueueWork(() -> {
                        if (player == null) return;
                        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
                        if (patch == null) return;
                        if (!action.selected().equals("request")) StyleState.select(player, patch, action.selected());
                        sync(player, patch);
                    });
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void request() { CHANNEL.sendToServer(new Action("request")); }
    public static void select(String id) { CHANNEL.sendToServer(new Action(id)); }

    public static void sync(ServerPlayer player, ServerPlayerPatch patch) {
        var learned = StyleState.learned(player, patch);
        StyleState.restoreEntries(player, patch);
        var rows = new ArrayList<Row>();
        for (var entry : StyleCatalogue.entries(patch)) {
            rows.add(new Row(entry.id(), entry.name(), List.copyOf(entry.sources()),
                    learned.contains(entry.id())));
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new Snapshot(List.copyOf(rows), StyleState.selected(player, patch)));
    }

    private static void encodeSnapshot(Snapshot snapshot, FriendlyByteBuf buffer) {
        buffer.writeVarInt(snapshot.rows().size());
        for (var row : snapshot.rows()) {
            buffer.writeUtf(row.id(), 64);
            buffer.writeUtf(row.name(), 120);
            buffer.writeVarInt(row.sources().size());
            for (var source : row.sources()) buffer.writeResourceLocation(source);
            buffer.writeBoolean(row.learned());
        }
        buffer.writeUtf(snapshot.selected(), 64);
    }

    private static Snapshot decodeSnapshot(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > 4096) throw new IllegalArgumentException("Invalid moveset catalogue size");
        var rows = new ArrayList<Row>(size);
        for (int i = 0; i < size; i++) {
            String id = buffer.readUtf(64);
            String name = buffer.readUtf(120);
            int sourceCount = buffer.readVarInt();
            if (sourceCount < 0 || sourceCount > 512) throw new IllegalArgumentException("Invalid source count");
            var sources = new ArrayList<ResourceLocation>(sourceCount);
            for (int j = 0; j < sourceCount; j++) sources.add(buffer.readResourceLocation());
            rows.add(new Row(id, name, List.copyOf(sources), buffer.readBoolean()));
        }
        return new Snapshot(List.copyOf(rows), buffer.readUtf(64));
    }
}
