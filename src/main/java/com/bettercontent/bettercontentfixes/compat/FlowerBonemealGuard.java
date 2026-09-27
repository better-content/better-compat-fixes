package com.bettercontent.bettercontentfixes.compat;

import com.bettercontent.bettercontentfixes.BetterContentFixes;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID)
public final class FlowerBonemealGuard {
    private FlowerBonemealGuard() {}

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (event.getBlock().is(BlockTags.FLOWERS)) {
            event.setResult(Event.Result.DENY);
        }
    }
}
