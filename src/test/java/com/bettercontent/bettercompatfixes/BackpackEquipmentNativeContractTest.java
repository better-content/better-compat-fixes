package com.bettercontent.bettercompatfixes;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

/** Reproducible upstream pins and final admission flow, not historical outputs or GUI acceptance. */
class BackpackEquipmentNativeContractTest {
    private static final String C = "top/theillusivec4/curios/";
    private static final String ROOT = "src/main/java/com/bettercontent/bettercompatfixes/";
    private static ZipFile pinned(String id, String sha1) throws Exception {
        var path = Path.of(System.getProperty("bc.backpackEquipment." + id));
        assertEquals(sha1, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path))));
        return new ZipFile(path.toFile());
    }
    private static ClassNode type(ZipFile zip, String name) throws Exception {
        var node = new ClassNode();
        try (var in = zip.getInputStream(zip.getEntry(name + ".class"))) { new ClassReader(in).accept(node, 0); }
        return node;
    }
    private static MethodNode method(ClassNode node, String name, String descriptor) {
        return node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(descriptor)).findFirst().orElseThrow();
    }
    private static int call(MethodNode method, String owner, String name) {
        int index = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode m && m.owner.equals(owner) && m.name.equals(name)) return index;
            index++;
        }
        return -1;
    }
    @Test void pinnedCuriosHandlersAdmitAfterEventsAndPredicatesAndKeepExtractionSeparate() throws Exception {
        try (var zip = pinned("6418456", "452175b95ad3db6ff58bb8968f6bf7a9d1e0f480")) {
            assertEquals("5.14.1+1.20.1", new java.util.jar.Manifest(zip.getInputStream(zip.getEntry("META-INF/MANIFEST.MF")))
                    .getMainAttributes().getValue("Implementation-Version"));
            var handler = type(zip, C + "common/inventory/DynamicStackHandler");
            assertEquals("net/minecraftforge/items/ItemStackHandler", handler.superName);
            assertTrue(handler.fields.stream().anyMatch(f -> f.name.equals("ctxBuilder") && f.desc.equals("Ljava/util/function/Function;")));
            var valid = method(handler, "isItemValid", "(ILnet/minecraft/world/item/ItemStack;)Z");
            int posted = call(valid, "net/minecraftforge/eventbus/api/IEventBus", "post");
            int result = call(valid, C + "api/event/CurioEquipEvent", "getResult");
            int predicate = call(valid, C + "api/CuriosApi", "isStackValid");
            assertTrue(posted >= 0 && result > posted && predicate > result);
            assertTrue(call(method(handler, "lambda$isItemValid$0", "(L" + C + "api/SlotContext;L" + C + "api/type/capability/ICurio;)Ljava/lang/Boolean;"),
                    C + "api/type/capability/ICurio", "canEquip") >= 0);
            assertTrue(java.util.Arrays.stream(valid.instructions.toArray()).filter(i -> i.getOpcode() == Opcodes.IRETURN).count() == 2);
            assertEquals(-1, call(method(handler, "extractItem", "(IIZ)Lnet/minecraft/world/item/ItemStack;"), handler.name, "isItemValid"));
            var api = type(zip, C + "mixin/core/MixinCuriosApi");
            var apiInjector = method(api, "curios$isStackValid", "(L" + C + "api/SlotContext;Lnet/minecraft/world/item/ItemStack;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V");
            assertTrue(call(apiInjector, C + "mixin/CuriosImplMixinHooks", "isStackValid") >= 0);
            assertTrue(call(apiInjector, "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable", "setReturnValue") >= 0,
                    "Native API cancels at HEAD; whole-method wrapper must include its injected early return");
            var implementation = type(zip, C + "mixin/CuriosImplMixinHooks");
            assertTrue(call(method(implementation, "isStackValid", "(L" + C + "api/SlotContext;Lnet/minecraft/world/item/ItemStack;)Z"),
                    implementation.name, "getItemStackSlots") >= 0);
            assertTrue(implementation.methods.stream().filter(m -> m.name.startsWith("lambda$getItemStackSlots$"))
                    .anyMatch(m -> call(m, C + "api/CuriosApi", "testCurioPredicates") >= 0));
            for (String name : new String[]{"CuriosContainer", "CuriosContainerV2"}) {
                var menu = type(zip, C + "common/inventory/container/" + name);
                assertTrue(menu.methods.stream().anyMatch(m -> m.name.equals("m_7648_") && m.desc.equals("(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;")));
                var armor = type(zip, menu.name + "$1");
                assertTrue(call(method(armor, "m_5857_", "(Lnet/minecraft/world/item/ItemStack;)Z"),
                        "net/minecraft/world/item/ItemStack", "canEquip") >= 0);
            }
        }
    }
    @Test void pinnedBackpackInheritsForgeCanEquipWithoutReplacingNativeUseOrEquipmentIdentity() throws Exception {
        try (var zip = pinned("7916619", "e971c86081ff00e5ade62d5b9aeb15d0905a8ded")) {
            assertEquals("1.20.1-3.24.35.1675", new java.util.jar.Manifest(zip.getInputStream(zip.getEntry("META-INF/MANIFEST.MF")))
                    .getMainAttributes().getValue("Implementation-Version"));
            String metadata = new String(zip.getInputStream(zip.getEntry("META-INF/mods.toml")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(metadata.contains("version=\"3.24.35.1675\""), "FML metadata version differs from archive Implementation-Version");
            var bag = type(zip, "net/p3pp3rf1y/sophisticatedbackpacks/backpack/BackpackItem");
            assertFalse(bag.methods.stream().anyMatch(m -> m.name.equals("canEquip")), "Hook relies on inherited Forge default");
            var equipment = method(bag, "getEquipmentSlot", "(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/EquipmentSlot;");
            assertTrue(java.util.Arrays.stream(equipment.instructions.toArray()).anyMatch(i -> i instanceof FieldInsnNode f && f.name.equals("CHEST")));
            method(bag, "m_7203_", "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;");
            method(bag, "tryPlace", "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/Direction;Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;");
            var items = type(zip, "net/p3pp3rf1y/sophisticatedbackpacks/init/ModItems");
            for (String tier : new String[]{"BACKPACK", "COPPER_BACKPACK", "IRON_BACKPACK", "GOLD_BACKPACK", "DIAMOND_BACKPACK", "NETHERITE_BACKPACK"})
                assertTrue(items.fields.stream().anyMatch(f -> f.name.equals(tier)), tier);
        }
    }
    @Test void nativeForgeArmorAndDispenserPathsConvergeOnTheGuardedAdmissions() throws Exception {
        java.util.function.Function<String, ClassNode> nativeType = name -> {
            try (var in = getClass().getClassLoader().getResourceAsStream(name + ".class")) {
                assertNotNull(in, name);
                var node = new ClassNode();
                new ClassReader(in).accept(node, 0);
                return node;
            } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        };
        var slot = nativeType.apply("net/minecraft/world/inventory/InventoryMenu$1");
        assertTrue(call(method(slot, "mayPlace", "(Lnet/minecraft/world/item/ItemStack;)Z"),
                "net/minecraft/world/item/ItemStack", "canEquip") >= 0);
        var stack = nativeType.apply("net/minecraftforge/common/extensions/IForgeItemStack");
        assertTrue(call(method(stack, "canEquip", "(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/Entity;)Z"),
                "net/minecraft/world/item/Item", "canEquip") >= 0);
        var selector = nativeType.apply("net/minecraft/world/entity/EntitySelector$MobCanWearArmorEntitySelector");
        assertTrue(call(method(selector, "test", "(Lnet/minecraft/world/entity/Entity;)Z"),
                "net/minecraft/world/entity/LivingEntity", "canTakeItem") >= 0);
        method(nativeType.apply("net/minecraft/world/entity/player/Player"), "canTakeItem", "(Lnet/minecraft/world/item/ItemStack;)Z");
        var dispenser = method(nativeType.apply("net/minecraft/world/item/ArmorItem"), "dispenseArmor",
                "(Lnet/minecraft/core/BlockSource;Lnet/minecraft/world/item/ItemStack;)Z");
        assertTrue(call(dispenser, selector.name, "<init>") >= 0);
    }
    @Test void hooksAreOptionalVersionPinnedFinalScopedAndNeverInterceptRemovalOrCommands() throws Exception {
        String plugin = Files.readString(Path.of(ROOT + "compat/plugin/BetterContentMixinPlugin.java"));
        assertTrue(plugin.contains("hasVersion(mods, \"curios\", \"5.14.1+1.20.1\")"));
        assertTrue(plugin.contains("hasVersion(mods, \"sophisticatedbackpacks\", \"3.24.35.1675\")"));
        String api = Files.readString(Path.of(ROOT + "mixin/backpackequipment/CuriosApiMixin.java"));
        assertTrue(api.contains("@WrapMethod"));
        assertTrue(api.indexOf("original.call(context, stack)") < api.indexOf("BackpackEquipmentPolicy.applies(context, stack)"));
        for (String hook : new String[]{"DynamicStackHandlerMixin", "PlayerMixin"}) {
            String source = Files.readString(Path.of(ROOT + "mixin/backpackequipment/" + hook + ".java"));
            assertTrue(source.contains("@At(\"RETURN\")"));
            assertTrue(source.contains("require = 1"));
            assertFalse(source.contains("setItemSlot"));
            assertFalse(source.contains("extractItem"));
            assertFalse(source.contains("setStackInSlot"));
        }
        String bag = Files.readString(Path.of(ROOT + "mixin/backpackequipment/BackpackItemMixin.java"));
        assertTrue(bag.contains("entity instanceof Player && slot.getType() == EquipmentSlot.Type.ARMOR"));
        assertTrue(bag.contains("return Mob.getEquipmentSlotForItem(stack) == slot"));
        String policy = Files.readString(Path.of(ROOT + "compat/BackpackEquipmentPolicy.java"));
        assertTrue(policy.contains("context.entity() instanceof Player"));
        assertTrue(policy.contains("backpack(stack) || context.identifier().equals(\"better_backpack\")"));
        assertTrue(policy.contains("return backpack(stack) && exclusiveSlot("));
        assertTrue(policy.contains("identifier.equals(\"better_backpack\") && index == 0 && !cosmetic"));
        String armor = Files.readString(Path.of(ROOT + "compat/BackpackArmorPolicy.java"));
        assertTrue(armor.contains("stack.getItem() instanceof BackpackItem"));
        assertFalse(armor.contains("top.theillusivec4"));
        String player = Files.readString(Path.of(ROOT + "mixin/backpackequipment/PlayerMixin.java"));
        assertTrue(player.contains("BackpackArmorPolicy.backpack(stack)"));
        assertFalse(player.contains("BackpackEquipmentPolicy"));
        int armorGate = plugin.indexOf("mixin.backpackequipment.PlayerMixin");
        int curiosGate = plugin.indexOf("mixinClassName.startsWith(\"com.bettercontent.bettercompatfixes.mixin.backpackequipment.\")");
        assertTrue(armorGate >= 0 && curiosGate > armorGate);
        assertFalse(plugin.substring(armorGate, curiosGate).contains("hasVersion(mods, \"curios\""));
        String build = Files.readString(Path.of("build.gradle.kts"));
        assertTrue(build.contains("if (!backpackWithoutCurios)"));
        assertTrue(build.contains("curiosPresent != backpackWithoutCurios"));
        String absence = Files.readString(Path.of(ROOT + "gametest/BackpackArmorGameTests.java"));
        assertFalse(absence.contains("top.theillusivec4"));
        assertTrue(absence.contains("!ModList.get().isLoaded(\"curios\")"));
        assertTrue(absence.contains("player.getSlot(102).set(stack.copy())"));
        assertTrue(policy.contains("PlayerLoggedInEvent"));
        assertFalse(policy.contains("setItemSlot"));
        assertFalse(policy.contains("setStackInSlot"));
    }
}
