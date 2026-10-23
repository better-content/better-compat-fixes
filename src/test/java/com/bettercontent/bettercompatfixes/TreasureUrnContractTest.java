package com.bettercontent.bettercompatfixes;

import com.bettercontent.bettercompatfixes.compat.TreasureUrnPending;
import com.bettercontent.bettercompatfixes.compat.TreasureUrnCompatibility;
import com.bettercontent.bettercompatfixes.compat.TreasureUrnPredicate;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

final class TreasureUrnContractTest {
    private static final String ORIGINAL = "net/mehvahdjukaar/supplementaries/common/block/blocks/UrnBlock";
    private static final String ORACLE = "com/bettercontent/bettercompatfixes/nativefixture/oracle/PinnedUrnOracle";
    private static CompoundTag dummy(BlockPos pos) {
        var tag = new CompoundTag(); tag.putString("id", "DUMMY");
        tag.putInt("x", pos.getX()); tag.putInt("y", pos.getY()); tag.putInt("z", pos.getZ()); return tag;
    }
    @Test void optionalAbsenceIsInertButPresentUnreviewedVersionOrClassPairFails() throws Exception {
        assertFalse(TreasureUrnCompatibility.checkVersion(false, null));
        assertTrue(TreasureUrnCompatibility.checkVersion(true, "1.20-3.1.43"));
        for (String version : new String[]{null, "", "1.20-3.1.42", "1.20-3.1.44"})
            assertThrows(IllegalStateException.class, () -> TreasureUrnCompatibility.checkVersion(true, version));
        List<List<String>> pairs = new ArrayList<>();
        for (String property : List.of("bc.urn.raw", "bc.urn.mapped")) try (var zip = new ZipFile(System.getProperty(property))) {
            var hashes = new ArrayList<String>();
            for (String name : List.of(ORIGINAL, "net/mehvahdjukaar/supplementaries/common/block/tiles/UrnBlockTile"))
                hashes.add(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(entry(zip, name + ".class"))));
            TreasureUrnCompatibility.checkClasses(hashes.get(0), hashes.get(1)); pairs.add(hashes);
            assertThrows(IllegalStateException.class, () -> TreasureUrnCompatibility.checkClasses("0".repeat(64), hashes.get(1)));
        }
        assertThrows(IllegalStateException.class, () -> TreasureUrnCompatibility.checkClasses(pairs.get(0).get(0), pairs.get(1).get(1)));
    }
    @Test void narrowLookupPredicateDoesNotAdmitOtherStatesLiveEntriesOrAuthoredData() {
        assertTrue(TreasureUrnPredicate.skipLookup(true, false, true));
        assertFalse(TreasureUrnPredicate.skipLookup(true, false, false));
        assertFalse(TreasureUrnPredicate.skipLookup(true, true, true));
        assertFalse(TreasureUrnPredicate.skipLookup(true, true, false));
        assertFalse(TreasureUrnPredicate.skipLookup(false, false, true));
        assertFalse(TreasureUrnPredicate.skipLookup(false, false, false));
        assertFalse(TreasureUrnPredicate.skipLookup(false, true, true));
        assertFalse(TreasureUrnPredicate.skipLookup(false, true, false));
    }
    @Test void syntheticMarkerRequiresExactKeysTypesCoordinatesAndNeverMutatesInput() {
        BlockPos pos = new BlockPos(7, -35, 19);
        for (boolean packed : new boolean[]{false, true}) {
            CompoundTag tag = dummy(pos); if (packed) tag.putBoolean("keepPacked", true);
            CompoundTag before = tag.copy();
            assertTrue(TreasureUrnPending.minimalDummy(tag, pos)); assertEquals(before, tag);
        }
        List<CompoundTag> invalid = new ArrayList<>();
        var real = dummy(pos); real.putString("id", "supplementaries:urn"); invalid.add(real);
        var extra = dummy(pos); extra.putString("LootTable", "minecraft:chests/simple_dungeon"); invalid.add(extra);
        var inventory = dummy(pos); inventory.put("Items", new net.minecraft.nbt.ListTag()); invalid.add(inventory);
        var owner = dummy(pos); owner.putString("Owner", "not-disposable"); invalid.add(owner);
        for (String axis : List.of("x", "y", "z")) {
            var wrongType = dummy(pos); wrongType.putLong(axis, wrongType.getInt(axis)); invalid.add(wrongType);
            var wrongPosition = dummy(pos); wrongPosition.putInt(axis, wrongPosition.getInt(axis) + 1); invalid.add(wrongPosition);
            var missing = dummy(pos); missing.remove(axis); invalid.add(missing);
        }
        for (int value : new int[]{0, 2, -1}) { var tag = dummy(pos); tag.putByte("keepPacked", (byte) value); invalid.add(tag); }
        var wrongPacked = dummy(pos); wrongPacked.putInt("keepPacked", 1); invalid.add(wrongPacked);
        var wrongId = dummy(pos); wrongId.putInt("id", 1); invalid.add(wrongId);
        assertFalse(TreasureUrnPending.minimalDummy(null, pos));
        assertTrue(TreasureUrnPending.absentOrMinimal(null, pos));
        for (CompoundTag tag : invalid) {
            CompoundTag before = tag.copy(); assertFalse(TreasureUrnPending.minimalDummy(tag, pos), tag.toString());
            assertFalse(TreasureUrnPending.absentOrMinimal(tag, pos)); assertEquals(before, tag);
        }
    }
    private static ClassNode read(byte[] bytes) { var node = new ClassNode(); new ClassReader(bytes).accept(node, 0); return node; }
    private static byte[] entry(ZipFile zip, String name) throws Exception {
        assertNotNull(zip.getEntry(name), name); try (var input = zip.getInputStream(zip.getEntry(name))) { return input.readAllBytes(); }
    }
    private static String normalized(Object value) { return String.valueOf(value).replace(ORACLE, ORIGINAL); }
    private static List<String> behavior(MethodNode method) {
        Map<LabelNode,Integer> labels = new IdentityHashMap<>();
        for (var instruction : method.instructions) if (instruction instanceof LabelNode label) labels.put(label, labels.size());
        List<String> result = new ArrayList<>();
        for (var i : method.instructions) {
            if (i.getOpcode() < 0) continue;
            String value = Integer.toString(i.getOpcode());
            if (i instanceof IntInsnNode n) value += ":" + n.operand;
            else if (i instanceof VarInsnNode n) value += ":" + n.var;
            else if (i instanceof TypeInsnNode n) value += ":" + n.desc;
            else if (i instanceof FieldInsnNode n) value += ":" + n.owner + ":" + n.name + ":" + n.desc;
            else if (i instanceof MethodInsnNode n) value += ":" + n.owner + ":" + n.name + ":" + n.desc + ":" + n.itf;
            else if (i instanceof InvokeDynamicInsnNode n) value += ":" + n.name + ":" + n.desc + ":" + n.bsm + ":" + Arrays.toString(n.bsmArgs);
            else if (i instanceof JumpInsnNode n) value += ":" + labels.get(n.label);
            else if (i instanceof LdcInsnNode n) value += ":" + n.cst;
            else if (i instanceof IincInsnNode n) value += ":" + n.var + ":" + n.incr;
            else if (i instanceof TableSwitchInsnNode n) value += ":" + n.min + ":" + n.max + ":" + labels.get(n.dflt) + ":" + n.labels.stream().map(labels::get).toList();
            else if (i instanceof LookupSwitchInsnNode n) value += ":" + n.keys + ":" + labels.get(n.dflt) + ":" + n.labels.stream().map(labels::get).toList();
            else if (i instanceof MultiANewArrayInsnNode n) value += ":" + n.desc + ":" + n.dims;
            result.add(normalized(value));
        }
        return result;
    }
    @Test void independentPinnedOriginalOracleHasNoBehaviorRewriting() throws Exception {
        ClassNode original;
        try (var zip = new ZipFile(System.getProperty("bc.urn.mapped"))) { original = read(entry(zip, ORIGINAL + ".class")); }
        ClassNode oracle = read(Files.readAllBytes(Path.of(System.getProperty("bc.urn.oracle"), ORACLE + ".class")));
        assertEquals(ORACLE, oracle.name); assertEquals(original.superName, oracle.superName);
        assertEquals(original.interfaces, oracle.interfaces); assertEquals(original.methods.size(), oracle.methods.size());
        assertEquals(original.fields.stream().map(f -> f.name + f.desc + f.access + f.value).toList(),
                oracle.fields.stream().map(f -> normalized(f.name + f.desc + f.access + f.value)).toList());
        for (int index = 0; index < original.methods.size(); index++) {
            var a = original.methods.get(index); var b = oracle.methods.get(index);
            assertEquals(a.name + a.desc + a.access, normalized(b.name + b.desc + b.access));
            assertEquals(behavior(a), behavior(b), "Original native oracle method changed: " + a.name);
        }
    }
    private static Object annotationValue(AnnotationNode annotation, String key) {
        int index = annotation.values.indexOf(key); assertTrue(index >= 0, key); return annotation.values.get(index + 1);
    }
    @Test void packagedTypedHooksRefmapAndForwardOnlyBodiesMatchBothNativeAbis() throws Exception {
        try (var zip = new ZipFile("build/libs/better-compat-fixes-0.1.9.jar")) {
            var config = JsonParser.parseString(new String(entry(zip, "better_compat_fixes.urn.mixins.json"), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(config.get("required").getAsBoolean());
            List<String> configured = new ArrayList<>(); for (var value : config.getAsJsonArray("mixins")) configured.add(value.getAsString());
            assertEquals(List.of("TreasureUrnWorldGenLookupMixin", "TreasureUrnLookupMixin"), configured);
            assertNull(zip.getEntry("com/bettercontent/bettercompatfixes/mixin/supplementaries/TreasureUrnStateMixin.class"));
            String refmap = new String(entry(zip, "better_compat_fixes.urn.refmap.json"), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(refmap.contains("m_7702_") && refmap.contains("WorldGenLookupMixin"), "Missing actual AP source/production region lookup mapping");
            assertFalse(refmap.contains("m_155947_"), "Global state predicate must never be patched");
            assertTrue(refmap.contains("m_5685_") && refmap.contains("EntityCreationType"), "Missing actual AP overloaded lookup mapping");
            for (String type : List.of("TreasureUrnWorldGenLookupMixin", "TreasureUrnLookupMixin")) {
                ClassNode node = read(entry(zip, "com/bettercontent/bettercompatfixes/mixin/supplementaries/" + type + ".class"));
                assertTrue(node.fields.isEmpty(), "No pending map shadow or final-write workaround");
                var hook = node.methods.stream().filter(m -> m.name.startsWith("betterContent$")).findFirst().orElseThrow();
                var annotation = hook.visibleAnnotations.stream().filter(a -> a.desc.endsWith("/Inject;")).findFirst().orElseThrow();
                for (String key : List.of("require", "expect", "allow")) assertEquals(1, annotationValue(annotation, key));
                for (var instruction : hook.instructions) if (instruction instanceof MethodInsnNode call) {
                    assertFalse(Set.of("remove", "clear", "put", "setBlockEntityNbt", "loadStatic", "promotePendingBlockEntity", "getBlockEntity", "m_7702_", "m_5685_").contains(call.name),
                            "Guard must never consume/mutate/replace data or decoder");
                }
            }
            assertNull(zip.getEntry(ORACLE + ".class"));
            assertNull(zip.getEntry("com/bettercontent/bettercompatfixes/gametest/TreasureUrnGameTests.class"));
            assertNull(zip.getEntry("com/bettercontent/bettercompatfixes/nativefixture/UrnHiddenItemsAccessor.class"));
            assertTrue(zip.stream().noneMatch(e -> e.getName().startsWith("urn-oracle/")));
        }
    }
}
