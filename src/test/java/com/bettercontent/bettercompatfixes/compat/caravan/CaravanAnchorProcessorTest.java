package com.bettercontent.bettercompatfixes.compat.caravan;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Ordinary pinned API/NBT controls only: no world/entity/bootstrap or Mixin-transform verdict. */
class CaravanAnchorProcessorTest {
    @Test void preserveAll2706OriginalOrdinaryNbtAssertionsOnActualProductProcessor() throws Exception {
        try (var stream = getClass().getResourceAsStream("/caravan/original-clown-caravan.nbt")) {
            assertNotNull(stream);
            assertEquals("615e6e3bf70263beb440ac405727b5dc7aa58a2af9307885421f548677e7eb71",
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes())));
        }
        assertEquals(2706, CaravanOriginalVectors.verify());
    }
    @Test void exactLeftKeyOnlyAndAppendOnceWithSameOtherObjects() {
        var original = new StructurePlaceSettings();
        for (var id : List.of(ResourceLocation.tryParse("other:clown_caravan"),
                ResourceLocation.tryParse("born_in_chaos_v1:clown_caravan_other")))
            assertSame(original, CaravanSettings.attach(Either.left(id), original));
        assertSame(original, CaravanSettings.attach(Either.right(new StructureTemplate()), original));
        var fixed = CaravanSettings.attach(Either.left(CaravanSettings.TEMPLATE), original);
        assertNotSame(original, fixed); assertTrue(original.getProcessors().isEmpty());
        assertEquals(List.of(CaravanAnchorProcessor.INSTANCE), fixed.getProcessors());
        assertSame(fixed, CaravanSettings.attach(Either.left(CaravanSettings.TEMPLATE), fixed));
    }
    @Test void copyKeepsSettingsExistingProcessorOrderAndSameUnconsumedRng() {
        var rng = RandomSource.create(9);
        var original = new StructurePlaceSettings().setRandom(rng).setMirror(Mirror.FRONT_BACK)
                .setRotation(Rotation.COUNTERCLOCKWISE_90).setRotationPivot(new BlockPos(9, 3, 1))
                .setBoundingBox(new BoundingBox(-8, -4, -2, 7, 9, 11)).setKeepLiquids(false)
                .setIgnoreEntities(false).setKnownShape(true).setFinalizeEntities(true);
        // Distinct ordinary marker processor without world/entity/registry access.
        var marker = new net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor() {
            @Override protected net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<?> getType() {
                return CaravanAnchorProcessor.TYPE;
            }
        };
        original.addProcessor(marker);
        var copy = CaravanSettings.attach(Either.left(CaravanSettings.TEMPLATE), original);
        assertEquals(List.of(marker), original.getProcessors());
        assertEquals(List.of(marker, CaravanAnchorProcessor.INSTANCE), copy.getProcessors());
        assertSame(rng, copy.getRandom(null)); assertEquals(RandomSource.create(9).nextLong(), rng.nextLong());
        assertSame(original.getBoundingBox(), copy.getBoundingBox());
        assertEquals(original.getMirror(), copy.getMirror()); assertEquals(original.getRotation(), copy.getRotation());
        assertEquals(original.getRotationPivot(), copy.getRotationPivot());
        assertEquals(original.shouldKeepLiquids(), copy.shouldKeepLiquids());
        assertEquals(original.isIgnoreEntities(), copy.isIgnoreEntities());
        assertEquals(original.getKnownShape(), copy.getKnownShape());
        assertEquals(original.shouldFinalizeEntities(), copy.shouldFinalizeEntities());
    }
    @Test void unitCodecRoundtripUsesTheRegisteredSingletonTypeIdentity() {
        var encoded = CaravanAnchorProcessor.CODEC.encodeStart(NbtOps.INSTANCE, CaravanAnchorProcessor.INSTANCE)
                .getOrThrow(false, error -> { throw new AssertionError(error); });
        var decoded = CaravanAnchorProcessor.CODEC.parse(NbtOps.INSTANCE, encoded)
                .getOrThrow(false, error -> { throw new AssertionError(error); });
        assertSame(CaravanAnchorProcessor.INSTANCE, decoded);
        assertSame(CaravanAnchorProcessor.TYPE, decoded.getType());
        assertSame(CaravanAnchorProcessor.CODEC, decoded.getType().codec());
        // Actual Forge registry attachment, and registry-dispatch roundtrip, require the future Native fixture.
    }
    @Test void unchangedProcessedPositionIdentityAndNoRawOrSharedNbtWrites() {
        var nbt = new CompoundTag(); nbt.putString("id", "minecraft:item_frame");
        nbt.putInt("TileX", -22); nbt.putInt("TileY", -57); nbt.putInt("TileZ", 11);
        var before = nbt.copy(); var pos = new BlockPos(25000003, -60, -25000008);
        var vec = new Vec3(25000003.5, -59.5, -25000008.25);
        var raw = new StructureTemplate.StructureEntityInfo(Vec3.ZERO, BlockPos.ZERO, nbt);
        var current = new StructureTemplate.StructureEntityInfo(vec, pos, nbt);
        var fixed = CaravanAnchorProcessor.INSTANCE.processEntity(null, null, raw, current, null, null);
        assertSame(pos, fixed.blockPos); assertSame(vec, fixed.pos); assertEquals(before, nbt);
        assertEquals(pos.getX(), fixed.nbt.getInt("TileX")); assertEquals(pos.getY(), fixed.nbt.getInt("TileY"));
        assertEquals(pos.getZ(), fixed.nbt.getInt("TileZ"));
    }
    @Test void everyMissingOrWrongTypedAnchorRemainsUnrepaired() {
        for (String field : List.of("TileX", "TileY", "TileZ")) for (boolean missing : List.of(true, false)) {
            var nbt = new CompoundTag(); nbt.putString("id", "minecraft:item_frame");
            nbt.putInt("TileX", 1); nbt.putInt("TileY", 2); nbt.putInt("TileZ", 3);
            if (missing) nbt.remove(field); else nbt.putByte(field, (byte) 1);
            var current = new StructureTemplate.StructureEntityInfo(Vec3.ZERO, BlockPos.ZERO, nbt);
            assertSame(current, CaravanAnchorProcessor.INSTANCE.processEntity(null, null, current, current, null, null));
        }
    }
}
