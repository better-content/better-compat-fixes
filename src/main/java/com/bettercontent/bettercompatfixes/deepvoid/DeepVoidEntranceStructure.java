package com.bettercontent.bettercompatfixes.deepvoid;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class DeepVoidEntranceStructure extends Structure {
    public static final Codec<DeepVoidEntranceStructure> CODEC = simpleCodec(DeepVoidEntranceStructure::new);
    // All inspected candidates have the same probability, including upstream revisions.
    public static final List<String> TEMPLATES = List.of(
            "cult_temple_noair3", "void_cult_temple", "cult_temple_noair2",
            "marrow_hole1", "marrow_hole2", "marrow_hole3", "marrow_hole3new",
            "deep_void_temple", "void_platform", "void_temple_new", "void_room", "void_room_new",
            "cult_ruins1_new", "cult_ruins2", "cult_ruins3_new", "cult_ruins4", "cult_ruins5_new", "cult_ruins6");

    public DeepVoidEntranceStructure(StructureSettings settings) { super(settings); }

    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        final var name = new ResourceLocation("the_deep_void", TEMPLATES.get(context.random().nextInt(TEMPLATES.size())));
        // The structure registry remains valid without the optional native mod, but no missing-template starts are made.
        if (context.structureTemplateManager().get(name).isEmpty()) return Optional.empty();
        final BlockPos bore = new BlockPos(context.chunkPos().getMiddleBlockX(),
                context.heightAccessor().getMinBuildHeight(), context.chunkPos().getMiddleBlockZ());
        return Optional.of(new GenerationStub(bore, pieces -> pieces.addPiece(
                new DeepVoidEntrancePiece(context.structureTemplateManager(), name, bore))));
    }

    @Override public StructureType<?> type() { return DeepVoidStructures.TYPE.get(); }
}
