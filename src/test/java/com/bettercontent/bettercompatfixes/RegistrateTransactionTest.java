package com.bettercontent.bettercompatfixes;

import com.bettercontent.bettercompatfixes.compat.registrate.RegistrateCompatibility;
import com.bettercontent.bettercompatfixes.compat.registrate.RegistrateTransaction;
import com.bettercontent.bettercompatfixes.compat.registrate.RegistrateMixinPlugin;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

/** Pure mechanism/ABI controls, not original native-call or transformation acceptance. */
public final class RegistrateTransactionTest {
    private static byte[][] classes(String path) throws Exception {
        try (var zip = new ZipFile(path)) {
            return new byte[][] {zip.getInputStream(zip.getEntry("com/tterrag/registrate/AbstractRegistrate.class")).readAllBytes(),
                zip.getInputStream(zip.getEntry("com/tterrag/registrate/AbstractRegistrate$Registration.class")).readAllBytes()};
        }
    }
    @Test public void independentActualClassPairsAbsencePartialAndUnknown() throws Exception {
        byte[][] raw = classes(System.getProperty("bc.registrate.raw")), mapped = classes(System.getProperty("bc.registrate.mapped"));
        assertFalse(RegistrateCompatibility.check(null, null));
        byte[][] other = classes(System.getProperty("bc.registrate.other.raw"));
        assertTrue(RegistrateCompatibility.check(raw[0], raw[1])); assertTrue(RegistrateCompatibility.check(mapped[0], mapped[1]));
        assertTrue(RegistrateCompatibility.check(other[0], other[1]));
        assertThrows(IllegalStateException.class, () -> RegistrateCompatibility.check(raw[0], other[1]));
        assertThrows(IllegalStateException.class, () -> RegistrateCompatibility.check(other[0], raw[1]));
        assertThrows(IllegalStateException.class, () -> RegistrateCompatibility.check(other[0], mapped[1]));
        assertThrows(IllegalStateException.class, () -> RegistrateCompatibility.check(raw[0], null));
        assertThrows(IllegalStateException.class, () -> RegistrateCompatibility.check(raw[0], mapped[1]));
        byte[] wrong = raw[0].clone(); wrong[wrong.length - 1] ^= 1;
        assertThrows(IllegalStateException.class, () -> RegistrateCompatibility.check(wrong, raw[1]));
    }
    @Test public void pluginInspectsResourcesWithoutLoadingTargets() throws Exception {
        byte[][] raw = classes(System.getProperty("bc.registrate.raw"));
        ClassLoader before = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(new ClassLoader(null) {
                @Override public InputStream getResourceAsStream(String name) {
                    if (name.equals("com/tterrag/registrate/AbstractRegistrate.class")) return new ByteArrayInputStream(raw[0]);
                    if (name.equals("com/tterrag/registrate/AbstractRegistrate$Registration.class")) return new ByteArrayInputStream(raw[1]);
                    return null;
                }
                @Override protected Class<?> loadClass(String name, boolean resolve) { throw new AssertionError("Plugin must not load/initialize target: " + name); }
            });
            assertTrue(new RegistrateMixinPlugin().shouldApplyMixin("unused", "unused"));
            Thread.currentThread().setContextClassLoader(new ClassLoader(null) {});
            assertFalse(new RegistrateMixinPlugin().shouldApplyMixin("unused", "unused"));
        } finally { Thread.currentThread().setContextClassLoader(before); }
    }
    @Test public void exceptionIdentityAndNestedBaselineCleanup() {
        var tx = new RegistrateTransaction(); RuntimeException exact = new RuntimeException("original");
        try {
            int baseline = tx.baseline();
            try { tx.begin(); throw exact; } finally { tx.restore(baseline); }
        } catch (RuntimeException original) { assertSame(exact, original); }
        assertEquals(0, tx.baseline());
        tx.begin();
        try { int baseline = tx.baseline(); try { tx.begin(); throw exact; } finally { tx.restore(baseline); } }
        catch (RuntimeException original) { assertSame(exact, original); assertEquals(1, tx.baseline()); }
        finally { tx.end(); }
        assertEquals(0, tx.baseline());
    }
    @Test public void unsupportedReentrantHashMayInheritOuterOwnership() {
        var tx = new RegistrateTransaction(); tx.begin();
        try {
            // A native callback hash could reenter a constructor before its OWN acquisition.
            int nestedBaseline = tx.baseline(); assertEquals(1, nestedBaseline);
            tx.restore(nestedBaseline); assertEquals(1, tx.baseline());
        } finally { tx.end(); }
        assertEquals(0, tx.baseline()); // Explicit unsupported case, never a deadlock-free claim.
    }
    public static void main(String[] args) throws Exception {
        var test = new RegistrateTransactionTest(); test.independentActualClassPairsAbsencePartialAndUnknown();
        test.pluginInspectsResourcesWithoutLoadingTargets(); test.exceptionIdentityAndNestedBaselineCleanup(); test.unsupportedReentrantHashMayInheritOuterOwnership();
        System.out.println("Registrate: 4 pure controls passed; transformation/native qualification pending");
    }
}
