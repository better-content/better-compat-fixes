package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.mixin.distanthorizons.AbstractDhTintGetterMixin;
import com.bettercontent.bettercompatfixes.mixin.distanthorizons.TintGetterOverrideMixin;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import net.minecraft.world.level.LevelReader;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

final class DhTintParentContextTest {
    private static final class ContextGetter extends AbstractDhTintGetterMixin implements DhTintParentContext {
        Object level;
        void wrapper(IClientLevelWrapper wrapper) { clientLevelWrapper = wrapper; }
        public void bettercompatfixes$setTintParent(Object level) { this.level = level; }
    }
    private static IClientLevelWrapper wrapper(Object level) {
        return (IClientLevelWrapper) Proxy.newProxyInstance(DhTintParentContextTest.class.getClassLoader(),
                new Class<?>[]{IClientLevelWrapper.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getWrappedMcObject")) return level;
                    throw new AssertionError("Unexpected context access: " + method);
                });
    }
    private static void update(AbstractDhTintGetterMixin getter) throws Exception {
        var hook = AbstractDhTintGetterMixin.class.getDeclaredMethod("bettercompatfixes$refreshTintContext",
                org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);
        hook.setAccessible(true);
        hook.invoke(getter, new Object[]{null});
    }
    @Test void eachUpdateRefreshesItsOwnContextAndNeverReusesAnOldDimension() throws Exception {
        Object overworld = new Object(), aether = new Object();
        var getter = new ContextGetter();
        getter.wrapper(wrapper(overworld)); update(getter); assertSame(overworld, getter.level);
        getter.wrapper(wrapper(aether)); update(getter); assertSame(aether, getter.level);
        getter.wrapper(wrapper(overworld)); update(getter); assertSame(overworld, getter.level);
        // Level-free tint getter does not need a parent or even query a level.
        update(new AbstractDhTintGetterMixin() {});
    }
    @Test void actualParentBridgeReplacesRatherThanCachesAndRejectsInvalidContext() throws Exception {
        LevelReader first = level(), second = level();
        var getter = new TintGetterOverrideMixin() {};
        var parent = TintGetterOverrideMixin.class.getDeclaredField("parent"); parent.setAccessible(true);
        getter.bettercompatfixes$setTintParent(first); assertSame(first, parent.get(getter));
        getter.bettercompatfixes$setTintParent(second); assertSame(second, parent.get(getter));
        assertThrows(NullPointerException.class, () -> getter.bettercompatfixes$setTintParent(null));
        assertThrows(ClassCastException.class, () -> getter.bettercompatfixes$setTintParent(new Object()));
    }
    private static LevelReader level() throws Exception {
        // No world is queried by this bridge. A minimal identity-only JVM implementation
        // avoids Proxy's eager ChunkStatus initialization and a fake game bootstrap.
        var writer = new org.objectweb.asm.ClassWriter(0);
        writer.visit(61, org.objectweb.asm.Opcodes.ACC_PUBLIC, "test/TintLevelIdentity", null,
                "java/lang/Object", new String[]{"net/minecraft/world/level/LevelReader"});
        var init = writer.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode(); init.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        init.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(org.objectweb.asm.Opcodes.RETURN); init.visitMaxs(1, 1); init.visitEnd(); writer.visitEnd();
        class Loader extends ClassLoader {
            Loader() { super(DhTintParentContextTest.class.getClassLoader()); }
            Class<?> identity() { byte[] bytes = writer.toByteArray(); return defineClass(null, bytes, 0, bytes.length); }
        }
        return (LevelReader) new Loader().identity().getConstructor().newInstance();
    }
    public static void main(String[] args) throws Exception {
        var test = new DhTintParentContextTest();
        test.eachUpdateRefreshesItsOwnContextAndNeverReusesAnOldDimension();
        test.actualParentBridgeReplacesRatherThanCachesAndRejectsInvalidContext();
    }
}
