package com.bettercontent.bettercompatfixes.tooltipfixture;

import com.bettercontent.bettercompatfixes.client.tooltip.ToolTooltipOwner;
import com.bettercontent.bettercompatfixes.client.tooltip.ToolTooltipCatalogue;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Nonshipping native rendering smoke. NOT the inventory/EMI/key/bench acceptance matrix. */
@Mod.EventBusSubscriber(modid = "better_compat_fixes", value = Dist.CLIENT)
public final class TooltipClientSmoke {
    private static boolean started, shown;
    private static int ticks;
    private TooltipClientSmoke() {}

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var minecraft = Minecraft.getInstance();
        if (!started && minecraft.screen != null && (minecraft.screen instanceof TitleScreen
                || minecraft.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) && minecraft.getOverlay() == null) {
            started = true;
            var settings = new LevelSettings("Tooltip smoke", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                    true, new GameRules(), WorldDataConfiguration.DEFAULT);
            minecraft.createWorldOpenFlows().createFreshLevel("tooltip-smoke-" + System.currentTimeMillis(), settings,
                    new WorldOptions(12345L, false, false), access -> access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                            .getOrThrow(WorldPresets.FLAT).createWorldDimensions());
        }
        if (started && !shown && minecraft.player != null && ++ticks > 100) {
            shown = true;
            minecraft.setScreen(new SmokeScreen());
        }
    }

    private static final class SmokeScreen extends Screen {
        private final List<ItemStack> fixtures = new ArrayList<>();
        private final List<Map<String, Object>> report = new ArrayList<>();
        private int frame, index;
        SmokeScreen() {
            super(Component.literal("TCon native hover smoke — not full pack acceptance"));
            for (String id : List.of("tconstruct:sword", "tconstruct:pickaxe", "tconstruct:crossbow", "tconstruct:plate_chestplate", "tconstruct:travelers_shield", "tconstruct:arrow")) {
                var item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
                if (!(item instanceof IModifiable modifiable)) continue;
                var stack = ToolBuildHandler.buildItemRandomMaterials(modifiable, RandomSource.create(12345));
                if (!stack.isEmpty()) fixtures.add(stack);
            }
            if (!fixtures.isEmpty()) {
                var broken = fixtures.get(0).copy();
                broken.getOrCreateTag().putBoolean(ToolStack.TAG_BROKEN, true);
                broken.setHoverName(Component.literal("Named broken sword"));
                fixtures.add(broken);
                var upgraded = ToolStack.copyFrom(fixtures.get(0));
                upgraded.addModifier(new ModifierId("tconstruct", "necrotic"), 2);
                upgraded.addModifier(new ModifierId("tconstruct", "jagged"), 1);
                fixtures.add(upgraded.createStack());
                var sign = (IModifiable)BuiltInRegistries.ITEM.get(new ResourceLocation("tconstruct", "battlesign"));
                var combatOnly = ToolBuildHandler.buildToolRandomMaterials(sign, RandomSource.create(12345));
                combatOnly.addModifier(new ModifierId("tconstruct", "jagged"), 1);
                combatOnly.addModifier(new ModifierId("tconstruct", "fortune"), 1);
                fixtures.add(combatOnly.createStack());
                var unbreakable = ToolStack.copyFrom(fixtures.get(0));
                unbreakable.addModifier(new ModifierId("tconstruct", "unbreakable"), 1);
                fixtures.add(unbreakable.createStack());
                var loaded = ToolStack.copyFrom(fixtures.get(2));
                loaded.getPersistentData().put(new ResourceLocation("tconstruct", "crossbow_ammo"),
                        new ItemStack(net.minecraft.world.item.Items.ARROW, 3).save(new net.minecraft.nbt.CompoundTag()));
                fixtures.add(loaded.createStack());
                var tank = ToolStack.copyFrom(fixtures.get(0));
                tank.addModifier(new ModifierId("tconstruct", "tank"), 1);
                slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper.TANK_HELPER.setFluid(tank,
                        new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 250));
                fixtures.add(tank.createStack());
                var iron = slimeknights.tconstruct.library.materials.definition.MaterialVariant.of(
                        slimeknights.tconstruct.library.materials.MaterialRegistry.getMaterial(new slimeknights.tconstruct.library.materials.definition.MaterialId("tconstruct", "iron")));
                for (String id : List.of("sword", "pickaxe")) {
                    var ordinary = ToolBuildHandler.createSingleMaterial((IModifiable)BuiltInRegistries.ITEM.get(new ResourceLocation("tconstruct", id)), iron);
                    if (!ordinary.isEmpty()) fixtures.add(ordinary);
                }
            }
        }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void tick() {
            if (fixtures.isEmpty()) throw new IllegalStateException("No assembled native fixtures");
            frame++;
            if (frame == 1) {
                minecraft.options.guiScale().set(index < fixtures.size() ? 2 : 3);
                GLFW.glfwSetWindowSize(minecraft.getWindow().getWindow(), index < fixtures.size() ? 1280 : 1600, index < fixtures.size() ? 900 : 1000);
                minecraft.resizeDisplay();
            }
            if (frame == 40) {
                var stack = fixtures.get(index % fixtures.size());
                var before = stack.save(new net.minecraft.nbt.CompoundTag());
                var normal = stack.getTooltipLines(minecraft.player, TooltipFlag.NORMAL);
                var advanced = stack.getTooltipLines(minecraft.player, TooltipFlag.ADVANCED);
                if (!normal.equals(advanced)) throw new IllegalStateException("F3+H changed owned tooltip");
                if (normal.size() <= 2) throw new IllegalStateException("Loaded fixture returned unavailable fallback");
                if (!normal.equals(stack.getTooltipLines(null, TooltipFlag.NORMAL))) throw new IllegalStateException("Player presence changed tooltip");
                float health = minecraft.player.getHealth();
                minecraft.player.setHealth(1);
                minecraft.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 100, 4));
                if (!normal.equals(stack.getTooltipLines(minecraft.player, TooltipFlag.NORMAL))) throw new IllegalStateException("Live health/buff changed baseline");
                minecraft.player.setHealth(health);
                minecraft.player.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST);
                var indexed = java.util.concurrent.CompletableFuture.supplyAsync(() -> stack.getTooltipLines(null, TooltipFlag.ADVANCED)).join();
                if (!normal.equals(indexed)) throw new IllegalStateException("Off-thread indexing changed tooltip");
                if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("battlesign")
                        && normal.stream().anyMatch(c -> c.getString().toLowerCase(java.util.Locale.ROOT).contains("mining")))
                    throw new IllegalStateException("Combat-only item leaked mining content");
                if (!before.equals(stack.save(new net.minecraft.nbt.CompoundTag()))) throw new IllegalStateException("Hover changed NBT");
                if (!ToolTooltipOwner.owns(stack)) throw new IllegalStateException("Ownership boundary missing");
                var row = new LinkedHashMap<String, Object>();
                row.put("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                row.put("gui_scale", minecraft.options.guiScale().get());
                row.put("rows", normal.stream().map(Component::getString).toList());
                row.put("rendered_lines_at_300", normal.stream().mapToInt(c -> font.split(c, Math.min(300, width - 24)).size()).sum());
                row.put("nbt_unchanged", true); row.put("f3h_identical", true);
                row.put("null_player_identical", true); row.put("live_health_strength_ignored", true); row.put("off_thread_identical", true);
                var tool = ToolStack.copyFrom(stack);
                row.put("unclassified_modifiers", tool.getModifierList().stream().map(m -> m.getId().toString())
                        .filter(id -> !ToolTooltipCatalogue.current().modifiers().containsKey(id)).toList());
                report.add(row);
                Screenshot.grab(minecraft.gameDirectory, "tooltip-smoke-" + index + ".png", minecraft.getMainRenderTarget(), message -> {});
            }
            if (frame > 80) {
                frame = 0;
                if (++index >= fixtures.size() * 2) {
                    try {
                        Files.writeString(Path.of(minecraft.gameDirectory.toString(), "tooltip-smoke.json"),
                                new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("scope", "Native generic-hover smoke only; inventory, EMI screens, physical keys and benches NOT VERIFIED", "fixtures", report,
                                        "loaded_registry_unclassified", slimeknights.tconstruct.library.modifiers.ModifierManager.INSTANCE.getAllLocations()
                                                .map(Object::toString).filter(id -> !ToolTooltipCatalogue.current().modifiers().containsKey(id)).sorted().toList())));
                    } catch (java.io.IOException e) { throw new IllegalStateException(e); }
                    minecraft.stop();
                }
            }
        }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            graphics.drawString(font, title, 12, 12, 0xFFFFFF);
            graphics.drawString(font, "Fixture " + (index + 1) + " / " + (fixtures.size() * 2), 12, 26, 0xAAAAAA);
            var stack = fixtures.get(index % fixtures.size());
            graphics.renderItem(stack, 30, 65);
            graphics.renderTooltip(font, stack, 52, 65);
        }
    }
}
