package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID)
public final class FlowerBonemealGuard {
    private static final ResourceLocation BUMBLEZONE = new ResourceLocation("the_bumblezone", "the_bumblezone");
    private FlowerBonemealGuard() {}

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        // The native flowers tag also contains food crops and tree foliage. Use the reviewed
        // decorative membership supplied by Cultivars; ordinary item placement is not intercepted.
        if (!event.getLevel().dimension().location().equals(BUMBLEZONE)
                && DecorativeFlowerPolicy.isDecorative(event.getBlock())) {
            event.setResult(Event.Result.DENY);
        }
    }
}
