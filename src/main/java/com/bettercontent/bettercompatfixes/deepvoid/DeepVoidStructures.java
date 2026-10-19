package com.bettercontent.bettercompatfixes.deepvoid;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** One registered structure, with uniform template selection inside its saved piece. */
public final class DeepVoidStructures {
    public static final ResourceLocation ID = new ResourceLocation(BetterContentFixes.MOD_ID, "deep_void_entrance");
    public static final ResourceKey<Structure> KEY = ResourceKey.create(Registries.STRUCTURE, ID);
    private static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, BetterContentFixes.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, BetterContentFixes.MOD_ID);
    public static final RegistryObject<StructureType<DeepVoidEntranceStructure>> TYPE = TYPES.register("deep_void_entrance", () -> () -> DeepVoidEntranceStructure.CODEC);
    public static final RegistryObject<StructurePieceType> PIECE = PIECES.register("deep_void_entrance", () -> (context, tag) -> new DeepVoidEntrancePiece(context.structureTemplateManager(), tag));

    private DeepVoidStructures() { }
    public static void register(IEventBus bus) {
        TYPES.register(bus);
        PIECES.register(bus);
    }
}
