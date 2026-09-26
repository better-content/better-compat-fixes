package com.bettercontent.bettercontentfixes.mixin.thefleshthathates;

import com.bettercontent.bettercontentfixes.compat.FleshSpreadPositionKey;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** TFTH writes printed positions for two maps, then tries to parse their keys as longs. */
@Mixin(targets = "net.mcreator.thefleshthathates.FleshBlockSpread", remap = false)
public abstract class FleshBlockSpreadMixin {
    @Redirect(method = "saveStateToNBT", at = @At(
            value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;toString()Ljava/lang/String;",
            remap = false), remap = false, require = 2)
    private String better_content_fixes$writePackedPosition(final BlockPos position) {
        return FleshSpreadPositionKey.encode(position);
    }

    @Redirect(method = "loadStateFromNBT", at = @At(
            value = "INVOKE", target = "Ljava/lang/Long;parseLong(Ljava/lang/String;)J",
            remap = false), remap = false, require = 2)
    private long better_content_fixes$readPrintedOrPackedPosition(final String key) {
        return FleshSpreadPositionKey.decode(key);
    }
}
