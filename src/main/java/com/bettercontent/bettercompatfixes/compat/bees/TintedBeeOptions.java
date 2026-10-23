package com.bettercontent.bettercompatfixes.compat.bees;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

/** NEW typed ID, never a changed payload on the legacy SimpleParticleType ID. */
public record TintedBeeOptions(int rgb) implements ParticleOptions {
    public static final int VERSION = 1, WIRE_BYTES = 4;
    public static final Codec<TintedBeeOptions> CODEC = Codec.INT.comapFlatMap(
            rgb -> rgb < 0 || rgb > 0xffffff ? com.mojang.serialization.DataResult.error(() -> "Not RGB24")
                    : com.mojang.serialization.DataResult.success(new TintedBeeOptions(rgb)),
            TintedBeeOptions::rgb).fieldOf("rgb").codec();
    public static final Deserializer<TintedBeeOptions> DESERIALIZER = new Deserializer<>() {
        @Override public TintedBeeOptions fromCommand(ParticleType<TintedBeeOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int rgb = reader.readInt();
            if (rgb < 0 || rgb > 0xffffff) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().createWithContext(reader, rgb);
            return new TintedBeeOptions(rgb);
        }
        @Override public TintedBeeOptions fromNetwork(ParticleType<TintedBeeOptions> type, FriendlyByteBuf buffer) {
            if (buffer.readableBytes() != WIRE_BYTES) throw new IllegalArgumentException("Noncanonical bee v1 payload length");
            if (buffer.readUnsignedByte() != VERSION) throw new IllegalArgumentException("Unsupported bee payload version");
            return new TintedBeeOptions(buffer.readUnsignedMedium());
        }
    };
    public TintedBeeOptions { if (rgb < 0 || rgb > 0xffffff) throw new IllegalArgumentException("Not RGB24"); }
    @Override public ParticleType<?> getType() { return SwarmParticles.TINTED_BEE.get(); }
    @Override public void writeToNetwork(FriendlyByteBuf buffer) { buffer.writeByte(VERSION); buffer.writeMedium(rgb); }
    @Override public String writeToString() { return "better_compat_fixes:colony_bee_tinted_v1 " + rgb; }
}
