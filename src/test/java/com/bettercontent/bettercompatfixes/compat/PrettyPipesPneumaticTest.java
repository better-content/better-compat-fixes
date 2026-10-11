package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.compat.prettypipes.PressurizerAirPolicy;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class PrettyPipesPneumaticTest {
    private static final Path MAIN = Path.of("src/main/java/com/bettercontent/bettercompatfixes");
    private ClassNode nativeType(String name) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/" + name + ".class")) {
            assertNotNull(input, "Pinned Gradle native fixture: " + name);
            ClassNode node = new ClassNode(); new ClassReader(input).accept(node, 0); return node;
        }
    }

    @Test void exactWholeStackBudgetAndOverflowBoundaries() {
        assertEquals(640, PressurizerAirPolicy.cost(64));
        for (int air : new int[] {0, 1999, 2000, 2639}) assertFalse(PressurizerAirPolicy.canPay(air, 1000, 64));
        assertTrue(PressurizerAirPolicy.canPay(2640, 1000, 64));
        assertTrue(PressurizerAirPolicy.canPay(2010, 1000, 1));
        assertFalse(PressurizerAirPolicy.canPay(Integer.MAX_VALUE, 1000, Integer.MAX_VALUE));
        assertFalse(PressurizerAirPolicy.canPay(3000, 1000, 0));
        assertFalse(PressurizerAirPolicy.canPay(3000, 1000, -1));
        assertFalse(PressurizerAirPolicy.canPay(3000, 0, 1));
        assertTrue(PressurizerAirPolicy.canPay(Integer.MAX_VALUE, 1000, 214748164));
        assertFalse(PressurizerAirPolicy.canPay(Integer.MAX_VALUE, Integer.MAX_VALUE, 1));
    }

    @Test void signedShortWireHalvesReconstructEveryBoundary() {
        for (int value : new int[] {0, 2000, 32767, 32768, 65535, 65536, 70000, -1, -1000, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertEquals(value, PressurizerAirPolicy.combine((short) PressurizerAirPolicy.low(value),
                    (short) PressurizerAirPolicy.high(value)));
        }
    }

    @Test void pinnedNativePressurizerTargetsAndRoutingContractExist() throws Exception {
        var type = nativeType("de/ellpeck/prettypipes/pressurizer/PressurizerBlockEntity");
        for (String name : List.of("pressurizeItem", "getCapability", "tick"))
            assertEquals(1, type.methods.stream().filter(m -> m.name.equals(name)).count(), name);
        assertEquals("(Lnet/minecraft/world/item/ItemStack;Z)Z", type.methods.stream()
                .filter(m -> m.name.equals("pressurizeItem")).findFirst().orElseThrow().desc);
        assertTrue(type.methods.stream().anyMatch(m -> m.name.equals("load") || m.name.equals("m_142466_")));
        assertTrue(type.methods.stream().anyMatch(m -> m.name.equals("saveAdditional") || m.name.equals("m_183515_")));
        var pipe = nativeType("de/ellpeck/prettypipes/pipe/PipeBlockEntity");
        var speed = pipe.methods.stream().filter(m -> m.name.equals("getItemSpeed")).findFirst().orElseThrow();
        assertTrue(java.util.stream.StreamSupport.stream(speed.instructions.spliterator(), false)
                .anyMatch(i -> i instanceof MethodInsnNode call && call.name.equals("pressurizeItem")));
        var gui = nativeType("de/ellpeck/prettypipes/pressurizer/PressurizerGui");
        assertTrue(gui.methods.stream().anyMatch(m -> m.name.equals("renderLabels") || m.name.equals("m_280003_")));
        var gauge = nativeType("me/desht/pneumaticcraft/client/render/pressure_gauge/PressureGaugeRenderer2D");
        assertTrue(gauge.methods.stream().anyMatch(m -> m.name.equals("drawPressureGauge")
                && m.desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;FFFFFII)V")));
        var machine = nativeType("me/desht/pneumaticcraft/common/capabilities/MachineAirHandler");
        assertTrue(machine.methods.stream().anyMatch(m -> m.name.equals("tick")));
        assertTrue(machine.methods.stream().anyMatch(m -> m.name.equals("serializeNBT")));
    }

    @Test void nativeCraftingTerminalUpgradeAddsNoExtraMeteorMaterial() throws Exception {
        try (var input = getClass().getResourceAsStream("/data/prettypipes/recipes/crafting_terminal.json")) {
            assertNotNull(input, "Pinned native recipe fixture");
            var recipe = JsonParser.parseReader(new InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("prettypipes:item_terminal", recipe.getAsJsonObject("key").getAsJsonObject("T").get("item").getAsString());
            assertEquals("minecraft:crafting_table", recipe.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString());
            assertEquals("minecraft:redstone", recipe.getAsJsonObject("key").getAsJsonObject("R").get("item").getAsString());
            assertEquals(3, recipe.getAsJsonObject("key").size());
            assertEquals(List.of(" C ", "RTR", " R "), recipe.getAsJsonArray("pattern").asList().stream().map(v -> v.getAsString()).toList());
        }
    }

    @Test void integrationHasNoFeFallbackAndUsesNativeGaugeOnly() throws Exception {
        String be = Files.readString(MAIN.resolve("mixin/prettypipes/PressurizerBlockEntityMixin.java"));
        String gui = Files.readString(MAIN.resolve("mixin/prettypipes/PressurizerGuiMixin.java"));
        String menu = Files.readString(MAIN.resolve("mixin/prettypipes/PressurizerContainerMixin.java"));
        String plugin = Files.readString(MAIN.resolve("mixin/BetterContentMixinPlugin.java"));
        String config = Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json"));
        assertTrue(be.contains("createTierOneAirHandler(PressurizerAirPolicy.VOLUME)"));
        assertTrue(be.contains("cap == ForgeCapabilities.ENERGY") && be.contains("cir.setReturnValue(LazyOptional.empty())"));
        assertTrue(be.contains("!simulate") && be.contains("synchronized (air)"));
        assertTrue(be.contains("level.isClientSide") && be.contains("getChunkNow("));
        assertFalse(be.contains("getChunk("));
        assertTrue(be.contains("super.invalidateCaps()") && be.contains("super.reviveCaps()"));
        assertTrue(be.contains("air.tick(tile)") && be.contains("serializeNBT()") && be.contains("deserializeNBT("));
        assertTrue(be.contains("tag.remove(\"energy\")") && be.contains("ci.cancel()"));
        assertTrue(menu.contains("addDataSlot(new DataSlot()"));
        assertTrue(menu.contains("PressurizerAirPolicy.low(") && menu.contains("PressurizerAirPolicy.high("));
        assertTrue(gui.contains("PressureGaugeRenderer2D.drawPressureGauge("));
        assertTrue(gui.contains("new WidgetTooltipArea(") && gui.contains("printManometerMessage("));
        for (String forbidden : List.of("drawNeedle", "drawScale", "EnergyPercentage", "info.prettypipes.energy", "EnergyStorage"))
            assertFalse(gui.contains(forbidden), forbidden);
        assertTrue(gui.contains("createTierOneAirHandler(PressurizerAirPolicy.VOLUME)"));
        assertTrue(gui.contains("bc$displayAir.deserializeNBT(state)"));
        assertTrue(plugin.contains("hasVersion(mods, \"prettypipes\", \"1.15.0\") && hasMods(mods, \"pneumaticcraft\")"));
        var json = JsonParser.parseString(config).getAsJsonObject();
        assertTrue(json.getAsJsonArray("client").asList().stream().anyMatch(v -> v.getAsString().equals("prettypipes.PressurizerGuiMixin")));
        assertFalse(json.getAsJsonArray("mixins").asList().stream().anyMatch(v -> v.getAsString().equals("prettypipes.PressurizerGuiMixin")));
        String entry = Files.readString(MAIN.resolve("BetterContentFixes.java"));
        assertTrue(entry.contains("isLoaded(\"prettypipes\") && ModList.get().isLoaded(\"pneumaticcraft\")"));
        assertTrue(entry.contains("PrettyPipesPneumaticGameTests.class"));
    }
}
