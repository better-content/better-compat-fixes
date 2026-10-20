package com.bettercontent.bettercompatfixes.mixin.minecraft;

import com.bettercontent.bettercompatfixes.api.NaturalSpawnContext;
import com.bettercontent.bettercompatfixes.api.event.GeologicalAmbientEligibilityEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;
import java.util.Optional;

/** Scope only vanilla's candidate calls: native offspring initialization and In Control's
 * NATURAL lava spawner deliberately do not inherit this habitat-substitution authority.
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerContextMixin {
    @WrapMethod(method = {"getRandomSpawnMobAt", "m_220429_"}, remap = false, require = 1)
    private static Optional<MobSpawnSettings.SpawnerData> bettercontent$selection(ServerLevel level,
            StructureManager structures, ChunkGenerator generator, MobCategory category, RandomSource random,
            BlockPos position, Operation<Optional<MobSpawnSettings.SpawnerData>> original) {
        return NaturalSpawnContext.during(NaturalSpawnContext.Stage.SELECTION, () -> original.call(level, structures, generator, category, random, position));
    }

    @WrapMethod(method = {"isValidSpawnPostitionForType", "m_220421_"}, remap = false, require = 1)
    private static boolean bettercontent$placement(ServerLevel level, MobCategory category, StructureManager structures,
            ChunkGenerator generator, MobSpawnSettings.SpawnerData data, BlockPos.MutableBlockPos position,
            double distance, Operation<Boolean> original) {
        return NaturalSpawnContext.during(NaturalSpawnContext.Stage.PLACEMENT, () -> original.call(level, category, structures, generator, data, position, distance));
    }

    @WrapMethod(method = {"isValidPositionForMob", "m_46991_"}, remap = false, require = 1)
    private static boolean bettercontent$position(ServerLevel level, Mob mob, double distance, Operation<Boolean> original) {
        return NaturalSpawnContext.during(NaturalSpawnContext.Stage.POSITION, () -> original.call(level, mob, distance));
    }

    @WrapOperation(method = {"canSpawnMobAt", "m_220436_"}, remap = false,
            at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z", remap = false), require = 1)
    private static boolean bettercontent$geologicalMembership(List<?> liveCandidates, Object selected,
            Operation<Boolean> original, @Local(argsOnly = true) ServerLevel level,
            @Local(argsOnly = true) MobCategory category, @Local(argsOnly = true) BlockPos position) {
        if (original.call(liveCandidates, selected)) return true;
        // Vanilla compares SpawnerData identity. Reloadable/reweighted geological lists use
        // fresh entries. Waive that identity test only if the final, post-event candidate list
        // still contains this type AND a consumer confirms actual-position eligibility.
        // Empty/cancelled lists and independently removed types therefore still veto admission.
        if (!NaturalSpawnContext.active() || category != MobCategory.MONSTER
                || !(selected instanceof MobSpawnSettings.SpawnerData data)
                || liveCandidates.stream().noneMatch(value -> value instanceof MobSpawnSettings.SpawnerData other && other.type == data.type)) return false;
        return GeologicalAmbientEligibilityEvent.eligible(level, position, data.type, MobSpawnType.NATURAL);
    }
}
