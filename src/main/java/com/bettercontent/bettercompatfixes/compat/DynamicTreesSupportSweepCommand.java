package com.bettercontent.bettercompatfixes.compat;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public final class DynamicTreesSupportSweepCommand {
    private DynamicTreesSupportSweepCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("better_compat_fixes_dt_support_sweep")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> run(ctx.getSource())));
    }

    private static int run(final CommandSourceStack source) {
        final ServerLevel level = source.getServer().overworld();
        final BlockPos origin = BlockPos.containing(source.getPosition()).offset(0, 4, 0);
        final List<String> failures = DynamicTreesSupportSweep.run(level, origin);
        if (failures.isEmpty()) {
            source.sendSuccess(() -> Component.literal("[BCFIXES] DT support sweep passed for all registered species."), true);
            return 1;
        }
        source.sendFailure(Component.literal("[BCFIXES] DT support sweep failures: " + String.join("; ", failures)));
        return 0;
    }
}
