package com.bettercontent.bettercompatfixes.compat;

import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fml.ModList;

/** Local client visuals only; no packets or server-side particle broadcast. */
public final class ChoppingParticles {
    private ChoppingParticles() {}

    public static boolean isWood(BlockState state) {
        return state.is(BlockTags.LOGS) || (ModList.get().isLoaded("dynamictrees")
                && state.getBlock() instanceof BranchBlock);
    }

    public static void emit(Minecraft mc, BlockPos pos, Direction face, int count) {
        if (mc.level == null || !(mc.hitResult instanceof BlockHitResult hit)
                || !hit.getBlockPos().equals(pos)) return;
        BlockState material = mc.level.getBlockState(pos);
        if (ModList.get().isLoaded("dynamictrees") && material.getBlock() instanceof BranchBlock branch) {
            material = branch.getPrimitiveLog().map(b -> b.defaultBlockState()).orElse(material);
        }
        // The ray intersection is on the actual branch shape, not the enclosing block cube.
        var cut = hit.getLocation().add(face.getStepX() * .015, face.getStepY() * .015, face.getStepZ() * .015);
        for (int i = 0; i < count; i++) {
            var random = mc.level.random;
            mc.particleEngine.add(new TerrainParticle(mc.level, cut.x, cut.y, cut.z,
                    face.getStepX() * .06 + (random.nextDouble() - .5) * .04,
                    face.getStepY() * .06 + .025,
                    face.getStepZ() * .06 + (random.nextDouble() - .5) * .04, material, pos)
                    .updateSprite(material, pos).scale(.6f));
        }
    }
}
