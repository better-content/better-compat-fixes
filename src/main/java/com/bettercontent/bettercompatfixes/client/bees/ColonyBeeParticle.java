package com.bettercontent.bettercompatfixes.client.bees;

import com.bettercontent.bettercompatfixes.BetterContentFixes;
import com.bettercontent.bettercompatfixes.compat.bees.SwarmParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Independently authored closed excursion with wing jitter; no Forestry code or assets copied. */
public final class ColonyBeeParticle extends TextureSheetParticle {
    public static final int LIFETIME = 80;
    private final double originX, originY, originZ, angle, radius;
    private TextureAtlasSprite bodySprite, detailSprite;
    private float bodyRed, bodyGreen, bodyBlue;

    public ColonyBeeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        originX = x;
        originY = y;
        originZ = z;
        angle = random.nextDouble() * Math.PI * 2;
        radius = 0.7 + random.nextDouble() * 1.2;
        lifetime = LIFETIME;
        quadSize = 0.055f;
        hasPhysics = false;
        pickSprite(sprites);
    }

    /** Delegation preserves the original constructor/path/RNG draw count: one particle, two masks. */
    public ColonyBeeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, int rgb) {
        this(level, x, y, z, sprites);
        if (rgb < 0 || rgb > 0xffffff) throw new IllegalArgumentException("Not RGB24");
        bodySprite = sprites.get(0, 1); detailSprite = sprites.get(1, 1);
        bodyRed = ((rgb >>> 16) & 255) / 255f; bodyGreen = ((rgb >>> 8) & 255) / 255f; bodyBlue = (rgb & 255) / 255f;
        setSprite(bodySprite);
    }

    @Override public void render(VertexConsumer vertices, Camera camera, float partialTick) {
        if (bodySprite == null) { super.render(vertices, camera, partialTick); return; }
        setSprite(bodySprite); setColor(bodyRed, bodyGreen, bodyBlue);
        super.render(vertices, camera, partialTick);
        setSprite(detailSprite); setColor(1f, 1f, 1f);
        super.render(vertices, camera, partialTick);
        setSprite(bodySprite); setColor(bodyRed, bodyGreen, bodyBlue);
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        if (++age >= lifetime) { remove(); return; }
        double phase = (double) age / lifetime;
        double excursion = Math.sin(Math.PI * phase) * radius;
        double heading = angle + phase * Math.PI * 2;
        setPos(originX + Math.cos(heading) * excursion,
                originY + Math.sin(Math.PI * phase) * 0.8 + Math.sin(age * 1.7) * 0.045,
                originZ + Math.sin(heading) * excursion);
        roll = (float) (Math.sin(age * 2.1) * 0.2);
        alpha = Math.min(1f, Math.min(age / 5f, (lifetime - age) / 10f));
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    @Mod.EventBusSubscriber(modid = BetterContentFixes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void providers(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(SwarmParticles.BEE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) ->
                    new ColonyBeeParticle(level, x, y, z, sprites));
            event.registerSpriteSet(SwarmParticles.TINTED_BEE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) ->
                    new ColonyBeeParticle(level, x, y, z, sprites, type.rgb()));
        }
    }
}
