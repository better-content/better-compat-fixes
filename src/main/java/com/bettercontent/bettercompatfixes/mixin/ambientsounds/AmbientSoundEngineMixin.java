package com.bettercontent.bettercompatfixes.mixin.ambientsounds;

import com.bettercontent.bettercompatfixes.compat.AmbientSoundPlaybackRecovery;
import com.bettercontent.bettercompatfixes.config.BcFixesConfig;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.ambientsounds.environment.AmbientEnvironment;
import team.creative.ambientsounds.sound.AmbientSound;
import team.creative.ambientsounds.sound.AmbientSoundEngine;

import java.util.Iterator;
import java.util.List;

@Mixin(value = AmbientSoundEngine.class, remap = false)
public abstract class AmbientSoundEngineMixin {
    @Shadow
    private List<AmbientSound.SoundStream> sounds;

    @Shadow
    public abstract SoundManager getManager();

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void better_compat_fixes$retireRejectedStreams(
            final AmbientEnvironment environment,
            final CallbackInfo callbackInfo) {
        if (!BcFixesConfig.ambientSoundsRetryRejectedStreams()) {
            return;
        }

        final SoundManager manager = getManager();
        synchronized (sounds) {
            final Iterator<AmbientSound.SoundStream> iterator = sounds.iterator();
            while (iterator.hasNext()) {
                final AmbientSound.SoundStream stream = iterator.next();
                if (!AmbientSoundPlaybackRecovery.shouldRetireUnstarted(
                        manager.isActive(stream), stream.hasPlayedOnce())) {
                    continue;
                }

                stream.onFinished();
                manager.stop(stream);
                iterator.remove();
            }
        }
    }
}
