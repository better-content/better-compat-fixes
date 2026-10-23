package com.bettercontent.bettercompatfixes.client.quark;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

/** Exact upstream archive contracts. These do not synthesize mouse input or claim visual acceptance. */
class NativeSortContractTest {
    private static final String ROOT = "src/main/java/com/bettercontent/bettercompatfixes/";
    private static final String HANDLER = "org/violetmoon/quark/base/client/handler/InventoryButtonHandler";
    private static Path pin(String id, String hash) throws Exception {
        String location = System.getProperty("bc.quarkSort." + id);
        assertNotNull(location, "Resolve the declared native fixture for this invocation: " + id);
        Path path = Path.of(location);
        assertEquals(hash, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path))));
        return path;
    }
    private static ClassNode type(ZipFile zip, String name) throws Exception {
        var entry = zip.getEntry(name + ".class");
        assertNotNull(entry, name);
        ClassNode node = new ClassNode();
        try (var input = zip.getInputStream(entry)) { new ClassReader(input).accept(node, 0); }
        return node;
    }
    private static MethodNode method(ClassNode type, String name) {
        return type.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow();
    }
    private static boolean calls(MethodNode method, String owner, String name) {
        for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) return true;
        return false;
    }
    private static boolean text(MethodNode method, String value) {
        for (var instruction : method.instructions)
            if (instruction instanceof LdcInsnNode literal && value.equals(literal.cst)) return true;
        return false;
    }
    @Test void exactQuarkMouseLoopAndPrivateProviderFieldsMatchAdapter() throws Exception {
        try (var zip = new ZipFile(pin("6427817", "9b0fb82995622a0070c00827c698c43b49d9d14b").toFile())) {
            var handler = type(zip, HANDLER);
            var mouse = method(handler, "mouseInputEvent");
            assertEquals("(Lorg/violetmoon/zeta/client/event/play/ZScreen$MouseButtonPressed$Pre;)V", mouse.desc);
            assertTrue(calls(mouse, HANDLER, "forGui"));
            assertTrue(calls(mouse, "java/util/function/Consumer", "accept"));
            assertFalse(calls(mouse, "org/violetmoon/zeta/client/event/play/ZScreen$MouseButtonPressed$Pre", "isCanceled"),
                    "Pinned native dispatcher does not stop after canceling one provider");
            assertTrue(text(method(handler, "initGui"), "CuriosScreen"));
            var config = type(zip, "org/violetmoon/quark/base/config/QuarkGeneralConfig");
            assertTrue(text(method(config, "<clinit>"), "top.theillusivec4.curios.client.gui.CuriosScreen"));
            assertFalse(text(method(config, "<clinit>"), "top.theillusivec4.curios.client.gui.CuriosScreenV2"));
            assertTrue(calls(method(config, "isScreenAllowed"), "java/util/List", "contains"));
            assertTrue(calls(method(handler, "initGui"), config.name, "isScreenAllowed"));
            assertFalse(text(method(handler, "forGui"), "CuriosScreen"), "Pinned keyboard/mouse discovery misses Curios");
            var holder = type(zip, HANDLER + "$ButtonProviderHolder");
            for (String field : new String[]{"keybind", "pressed", "module", "enableCond"})
                assertTrue(holder.fields.stream().anyMatch(candidate -> candidate.name.equals(field)), field);
            assertTrue(handler.fields.stream().anyMatch(field -> field.name.equals("providers")
                    && field.desc.equals("Lcom/google/common/collect/Multimap;")));
        }
    }
    @Test void nativeCallbacksRetainSortingSettingsSoundAndServerPacketAuthority() throws Exception {
        try (var zip = new ZipFile(pin("6427817", "9b0fb82995622a0070c00827c698c43b49d9d14b").toFile())) {
            var client = type(zip, "org/violetmoon/quark/content/management/module/InventorySortingModule$Client");
            for (String suffix : new String[]{"0", "3", "6"}) {
                var callback = method(client, "lambda$registerKeybinds$" + suffix);
                assertTrue(calls(callback, "org/violetmoon/zeta/client/ZetaClient", "sendToServer"));
                assertTrue(calls(callback, "org/violetmoon/quark/base/network/message/SortInventoryMessage", "<init>"));
                assertTrue(calls(callback, client.name, "click"));
            }
            assertTrue(text(method(client, "registerKeybinds"), "quark.keybind.sort_player"));
            assertTrue(text(method(client, "registerKeybinds"), "quark.keybind.sort_container"));
        }
    }
    @Test void nativeSophisticatedSortHandlerIsIndependentAndUntouched() throws Exception {
        try (var zip = new ZipFile(pin("7916595", "5d5af8ec2db0ca4f8f1ae6c623da5c8176648746").toFile())) {
            var client = type(zip, "net/p3pp3rf1y/sophisticatedcore/client/ClientEventHandler");
            assertTrue(calls(method(client, "handleGuiMouseKeyPress"), client.name, "tryCallSort"));
            assertTrue(calls(method(client, "tryCallSort"),
                    "net/p3pp3rf1y/sophisticatedcore/common/gui/StorageContainerMenuBase", "sort"));
            assertNotNull(type(zip, "net/p3pp3rf1y/sophisticatedcore/client/gui/StorageScreenBase"));
        }
    }
    @Test void pinnedZetaMouseCoordinatesAndBothCuriosScreensAreNativeInputs() throws Exception {
        try (var zip = new ZipFile(pin("7335229", "8c4a8ef0444b53ff408d5b89126285031081d92f").toFile())) {
            var mouse = type(zip, "org/violetmoon/zeta/client/event/play/ZScreen$MouseButtonPressed");
            assertEquals("()D", method(mouse, "getMouseX").desc);
            assertEquals("()D", method(mouse, "getMouseY").desc);
            assertEquals("()I", method(mouse, "getButton").desc);
        }
        try (var zip = new ZipFile(pin("6418456", "452175b95ad3db6ff58bb8968f6bf7a9d1e0f480").toFile())) {
            for (String name : new String[]{"CuriosScreen", "CuriosScreenV2"})
                assertEquals("net/minecraft/client/gui/screens/inventory/EffectRenderingInventoryScreen",
                        type(zip, "top/theillusivec4/curios/client/gui/" + name).superName);
        }
    }
    @Test void clientOnlyHookSuppressesOriginalWithoutCancelingUnsortedEvents() throws Exception {
        var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources/better_compat_fixes.mixins.json"))).getAsJsonObject();
        for (String name : new String[]{"quark.SortProviderAccessor", "quark.InventoryButtonHandlerMixin", "quark.QuarkGeneralConfigMixin"}) {
            assertTrue(json.getAsJsonArray("client").asList().stream().anyMatch(value -> value.getAsString().equals(name)));
            assertFalse(json.getAsJsonArray("mixins").asList().stream().anyMatch(value -> value.getAsString().equals(name)));
        }
        String mixin = Files.readString(Path.of(ROOT + "mixin/quark/InventoryButtonHandlerMixin.java"));
        assertTrue(mixin.contains("event.getButton() != 2"));
        assertTrue(mixin.contains("ownsMiddleBinding(providers)"));
        assertTrue(mixin.indexOf("ci.cancel();") < mixin.indexOf("NativeSortRouting.route(event, providers)"));
        assertTrue(mixin.contains("require = 1, allow = 1"));
        String routing = Files.readString(Path.of(ROOT + "client/quark/NativeSortRouting.java"));
        assertTrue(routing.contains("getCarried().isEmpty()"));
        assertTrue(routing.contains("getSlotUnderMouse()"));
        assertTrue(routing.contains("getChildAt(event.getMouseX(), event.getMouseY()).isPresent()"));
        assertTrue(routing.contains("sophisticated(raw)"));
        assertTrue(routing.contains("ModList.get().isLoaded(\"sophisticatedcore\") && screen instanceof StorageScreenBase<?>"));
        assertTrue(routing.contains("name.equals(\"top.theillusivec4.curios.client.gui.CuriosScreen\")"));
        assertTrue(routing.contains("name.equals(\"top.theillusivec4.curios.client.gui.CuriosScreenV2\")"));
        for (String reflectiveDiscovery : new String[]{"getSuperclass", "Class.forName", "getDeclaredMethod", "getDeclaredField"})
            assertFalse(routing.contains(reflectiveDiscovery), reflectiveDiscovery);
        assertTrue(routing.contains("provider.bc$pressed().accept(screen);\n            event.setCanceled(true);\n            return;"));
        assertFalse(routing.contains("sendToServer"));
        assertFalse(routing.contains("new SortInventoryMessage"));
        String admission = Files.readString(Path.of(ROOT + "mixin/quark/QuarkGeneralConfigMixin.java"));
        assertTrue(admission.contains("getClass().getName().equals(\"top.theillusivec4.curios.client.gui.CuriosScreenV2\")"));
        assertTrue(admission.contains("cir.setReturnValue(true)"));
        assertTrue(admission.contains("method = \"isScreenAllowed\""));
        assertTrue(admission.contains("require = 1, allow = 1"));
        assertFalse(admission.contains("startsWith("));
        assertTrue(routing.contains("QuarkGeneralConfig.isScreenAllowed(raw)"));
        String plugin = Files.readString(Path.of(ROOT + "compat/plugin/BetterContentMixinPlugin.java"));
        assertTrue(plugin.contains("mixinClassName.startsWith(\"com.bettercontent.bettercompatfixes.mixin.quark.\")"));
        assertTrue(plugin.contains("hasVersion(mods, \"quark\", \"4.0-462\")"));
        assertTrue(plugin.contains("hasVersion(mods, \"zeta\", \"1.0-31\")"));
    }
}
