package com.bettercontent.bettercompatfixes.nativefixture;

import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Nonshipping typed access to the mailbox of a test-owned worker, never the world's worker. */
@Mixin(IOWorker.class)
public interface IOWorkerMailboxAccessor {
    @Accessor(value = "mailbox", remap = false)
    ProcessorMailbox<StrictQueue.IntRunnable> better_compat_fixes$mailbox();
}
