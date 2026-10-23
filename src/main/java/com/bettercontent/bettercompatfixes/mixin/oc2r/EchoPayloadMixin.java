package com.bettercontent.bettercompatfixes.mixin.oc2r;

import com.llamalad7.mixinextras.sugar.Local;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.nio.ByteBuffer;

/** Preserve native consumption, but never lend the caller's buffer to the asynchronous fallback. */
@Pseudo
@Mixin(targets = "li.cil.oc2.common.inet.DefaultSessionLayer", remap = false)
public abstract class EchoPayloadMixin {
    @ModifyVariable(method = "sendSession(Lli/cil/oc2/api/inet/session/Session;Ljava/nio/ByteBuffer;)V",
            at = @At(value = "FIELD", target = "Lli/cil/oc2/common/Main;LoadedLibrary:Z", opcode = Opcodes.GETSTATIC),
            argsOnly = true, ordinal = 0, require = 1, expect = 1, allow = 1, remap = false)
    private ByteBuffer betterContent$ownEchoPayload(ByteBuffer consumed, @Local(ordinal = 0) byte[] payload) {
        // The pinned site is AFTER get(payload). Neither the caller's position nor its storage
        // is rewound/retained. The native branch still uses the unmodified array and length.
        return com.bettercontent.bettercompatfixes.compat.Oc2rEchoPayload.captured(payload);
    }
}
