package com.bettercontent.bettercompatfixes.compat.moonlightplatform;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Qualified Moonlight owner only; never reserves normal helpers under a Mixin prefix. */
public final class MoonlightPlatformPlugin implements IMixinConfigPlugin {
    public static final String TARGET = "net.mehvahdjukaar.moonlight.forge.MoonlightForge";
    public static final String MIXIN = "com.bettercontent.bettercompatfixes.mixin.moonlightplatform.MoonlightForgePlatformMixin";
    public static final String METHOD = "lambda$new$1";
    public void onLoad(String prefix) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        if (!TARGET.equals(target) || !MIXIN.equals(mixin)) return false;
        // Unknown Moonlight is left completely untouched, including its diagnostic.
        try { return ownerClass() != null; }
        catch (IOException unreadable) { return false; }
    }
    private static byte[] ownerClass() throws IOException {
        var loading = LoadingModList.get();
        if (loading == null) return null;
        var owner = loading.getModFileById("moonlight");
        if (owner == null) return null;
        byte[] archive = NativeForgeApiIdentity.read(owner.getFile().getFilePath());
        String hash = NativeForgeApiIdentity.hash(archive);
        var pair = hash.equals(NativeForgeApiIdentity.RAW.moonlight()) ? NativeForgeApiIdentity.RAW
                : !FMLEnvironment.production && hash.equals(NativeForgeApiIdentity.SDK.moonlight()) ? NativeForgeApiIdentity.SDK : null;
        if (pair == null) return null;
        byte[] bytes = NativeForgeApiIdentity.entries(archive).get(NativeForgeApiIdentity.MOONLIGHT_CLASS);
        if (bytes == null || !pair.moonlightClass().equals(NativeForgeApiIdentity.hash(bytes))) return null;
        return bytes;
    }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        try {
            if (!TARGET.equals(target) || !MIXIN.equals(mixin)) throw new IllegalStateException("Unexpected Moonlight platform anchor");
            byte[] original = ownerClass();
            if (original == null) throw new IllegalStateException("Moonlight owner changed during preparation");
            verifySite(original, node);
        } catch (IOException unreadable) { throw new IllegalStateException("Moonlight owner became unreadable", unreadable); }
    }
    public static void verifySite(byte[] original, ClassNode actual) {
        String hash = NativeForgeApiIdentity.hash(original);
        if (!Set.of(NativeForgeApiIdentity.RAW.moonlightClass(), NativeForgeApiIdentity.SDK.moonlightClass()).contains(hash))
            throw new IllegalStateException("Unknown Moonlight input class ABI");
        ClassNode expected = new ClassNode(); new ClassReader(original).accept(expected, 0);
        if (!expected.name.equals(actual.name) || !TARGET.replace('.', '/').equals(actual.name)) throw new IllegalStateException("Wrong Moonlight target");
        MethodNode before = selected(expected), after = selected(actual);
        if (!Arrays.equals(methodBytes(before), methodBytes(after))) throw new IllegalStateException("Moonlight setup body drift");
        long queries = Arrays.stream(after.instructions.toArray()).filter(i -> i instanceof MethodInsnNode call
                && call.getOpcode() == Opcodes.INVOKEVIRTUAL && call.owner.equals("net/minecraftforge/fml/ModList")
                && call.name.equals("isLoaded") && call.desc.equals("(Ljava/lang/String;)Z")).count();
        long poi = Arrays.stream(after.instructions.toArray()).filter(i -> i instanceof MethodInsnNode call
                && call.owner.equals("net/mehvahdjukaar/moonlight/api/platform/RegHelper") && call.name.equals("addBlocksToPOI")).count();
        if (queries != 1 || poi != 1) throw new IllegalStateException("Moonlight callsite/POI cardinality drift");
        // No ASM mutation: Mixin Redirect replaces precisely this call; all original branch code remains.
    }
    private static MethodNode selected(ClassNode node) {
        var methods = node.methods.stream().filter(m -> m.name.equals(METHOD) && m.desc.equals("()V")).toList();
        if (methods.size() != 1) throw new IllegalStateException("Moonlight setup member count drift");
        return methods.get(0);
    }
    private static byte[] methodBytes(MethodNode method) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "bc/MoonlightSetupWitness", null, "java/lang/Object", null);
        method.accept(writer); writer.visitEnd(); return writer.toByteArray();
    }
    public void acceptTargets(Set<String> mine, Set<String> other) {}
    public List<String> getMixins() { return null; }
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
