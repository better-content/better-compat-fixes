package com.bettercontent.bettercompatfixes.registratefixture;

import com.bettercontent.bettercompatfixes.registratefixture.mixin.RegistrateStateAccessor;
import com.bettercontent.bettercompatfixes.registratefixture.mixin.RegistrationCallbacksAccessor;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.slf4j.LoggerFactory;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Actual transformed upstream Base calls only. Capture-only entries never register fake items/providers. */
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = "better_compat_fixes", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RegistrateGameTests {
    @SubscribeEvent public static void register(RegisterGameTestsEvent event) { event.register(RegistrateGameTests.class); }
    private static void require(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
    private static final class Registrar extends AbstractRegistrate<Registrar> {
        final AtomicInteger creators = new AtomicInteger();
        Registrar() { super("better_compat_fixes"); }
        RegistryEntry<Item> capture(String name, Runnable factory) {
            return accept(name, Registries.ITEM, null, () -> { creators.incrementAndGet(); return Items.STONE; }, object -> {
                factory.run(); return new RegistryEntry<>(this, object);
            });
        }
    }
    private static Thread thread(String name, AtomicReference<Throwable> failure, Runnable body) {
        return new Thread(() -> { try { body.run(); } catch (Throwable error) { failure.compareAndSet(null, error); } }, name);
    }
    private static void join(Thread thread) {
        try { thread.join(3000); if (thread.isAlive()) throw new AssertionError("Native Registrate thread deadline: " + thread.getName()); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
    }
    private static void cleanup(Thread... threads) {
        for (Thread t : threads) if (t != null && t.isAlive()) t.interrupt();
        for (Thread t : threads) if (t != null) join(t);
    }
    private static void noFailure(AtomicReference<Throwable> failure) {
        if (failure.get() != null) throw new AssertionError("Actual upstream call failed", failure.get());
    }
    private static void requireRuntimeSdk() {
        String[] resources = {"com/tterrag/registrate/AbstractRegistrate.class",
                "com/tterrag/registrate/AbstractRegistrate$Registration.class"};
        String[] expected = {"923bc6c760a84def349071c986044e7906503824c3ac460166f77a88973b65bc",
                "f150c0f07209434ae135ceb32480b22d05c539fd81c60e1e49605cd0c8e86864"};
        try {
            for (int i = 0; i < resources.length; i++) {
                try (var input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resources[i])) {
                    require(input != null, "Actual runtime Registrate SDK resource missing");
                    byte[] bytes = input.readNBytes(2 * 1024 * 1024 + 1);
                    require(bytes.length <= 2 * 1024 * 1024, "Actual runtime Registrate SDK resource oversized");
                    String actual = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
                    require(actual.equals(expected[i]), "Native fixture must exercise the exact official-mapped 1.3.11 SDK");
                }
            }
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException failure) { throw new AssertionError(failure); }
    }
    private static void witness(String test) {
        requireRuntimeSdk();
        String attempt = System.getProperty("bc.registrate.fixture.attempt", "");
        require(attempt.matches("[0-9a-f-]{36}"), "Missing fresh native attempt binding");
        LoggerFactory.getLogger(RegistrateGameTests.class).info("BC_REGISTRATE_NATIVE_V1 attempt={} case={} original_base=true sdk=MC1.20-1.3.11", attempt, test);
    }
    @GameTest(templateNamespace = "better_compat_fixes", template = "empty", batch = "registrate", timeoutTicks = 400)
    public static void pendingTransferReadersAndIdentity(GameTestHelper helper) {
        // Warm upstream RegistryEntry's own SDK initialization on native main before foreign threads.
        RegistryEntry.empty();
        Registrar owner = new Registrar();
        var view = (RegistrateStateAccessor) (Object) owner;
        Object originalMap = view.bc$pending(), originalTable = view.bc$registrations();
        NonNullConsumer<Item> first = value -> {}, second = value -> {}, late = value -> {};
        AtomicReference<Throwable> failure = new AtomicReference<>(); CountDownLatch start = new CountDownLatch(1);
        Thread one = thread("registrate-pending-a", failure, () -> { TransferGate.await(start); owner.addRegisterCallback("entry", Registries.ITEM, first); });
        Thread two = thread("registrate-pending-b", failure, () -> { TransferGate.await(start); owner.addRegisterCallback("entry", Registries.ITEM, second); });
        one.start(); two.start(); start.countDown();
        try { join(one); join(two); noFailure(failure); }
        finally { cleanup(one, two); }
        require(view.bc$pending().size() == 2, "Actual pending callbacks lost");
        require(view.bc$pending().asMap().values().stream().mapToInt(Collection::size).sum() == 2, "Actual pending accounting differs");
        TransferGate gate = new TransferGate(owner); TransferGate.arm(gate);
        AtomicReference<RegistryEntry<Item>> captured = new AtomicReference<>(); CountDownLatch adding = new CountDownLatch(1);
        Thread accept = thread("registrate-original-accept", failure, () -> captured.set(owner.capture("entry", () -> {})));
        Thread add = thread("registrate-during-transfer", failure, () -> { adding.countDown(); owner.addRegisterCallback("entry", Registries.ITEM, late); });
        accept.start();
        try {
            TransferGate.await(gate.reached); add.start(); TransferGate.await(adding);
            long deadline = System.nanoTime() + 2_000_000_000L; boolean blocked = false;
            var bean = ManagementFactory.getThreadMXBean();
            while (System.nanoTime() < deadline) {
                var info = bean.getThreadInfo(add.getId());
                if (info != null && info.getLockOwnerId() == accept.getId()) { blocked = true; break; }
                Thread.onSpinWait();
            }
            require(blocked, "Actual callback lookup did not wait for original transfer/publication transaction");
        } finally {
            TransferGate.clear(gate);
            try { join(accept); if (add.getState() != Thread.State.NEW) join(add); }
            finally { cleanup(accept, add); }
        }
        noFailure(failure);
        require(view.bc$pending().isEmpty() && view.bc$pending().asMap().isEmpty(), "Original transfer stranded callbacks");
        Object registration = view.bc$registrations().rowMap().values().iterator().next().values().iterator().next();
        List<?> callbacks = ((RegistrationCallbacksAccessor) registration).bc$callbacks();
        require(callbacks.size() == 3 && callbacks.stream().anyMatch(c -> c == first) && callbacks.stream().anyMatch(c -> c == second)
            && callbacks.get(2) == late, "Original callback identities/append order changed");
        require(owner.get("entry", Registries.ITEM) == captured.get(), "Actual get/delegate identity changed");
        Collection<?> snapshot = owner.getAll(Registries.ITEM); require(snapshot.size() == 1 && snapshot.iterator().next() == captured.get(), "Actual eager getAll identity");
        owner.capture("next", () -> {}); require(snapshot.size() == 1 && owner.getAll(Registries.ITEM).size() == 2, "getAll became a live/replaced view");
        require(view.bc$pending() == originalMap && view.bc$registrations() == originalTable, "Original collections replaced");
        require(owner.creators.get() == 0, "Native creator executed during capture");
        witness("pending-transfer"); helper.succeed();
    }
    @GameTest(templateNamespace = "better_compat_fixes", template = "empty", batch = "registrate", timeoutTicks = 400)
    public static void foreignFactoryAndExceptionalUnlock(GameTestHelper helper) {
        RegistryEntry.empty(); Registrar owner = new Registrar(); AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean foreignFinished = new AtomicBoolean(); AtomicReference<Thread> foreign = new AtomicReference<>();
        try {
            owner.capture("factory", () -> {
                Thread reader = thread("registrate-factory-foreign-reader", failure, () -> { require(owner.getAll(Registries.ITEM).isEmpty(), "Constructor not yet published"); foreignFinished.set(true); });
                foreign.set(reader); reader.start(); join(reader); noFailure(failure);
                require(foreignFinished.get(), "Foreign native getAll blocked under constructor lock");
            });
        } finally { if (foreign.get() != null) cleanup(foreign.get()); }
        RuntimeException exact = new RuntimeException("native-original-factory");
        try { owner.capture("factory-fault", () -> { throw exact; }); throw new AssertionError("Missing original factory failure"); }
        catch (RuntimeException actual) { require(actual == exact, "Original factory exception identity changed"); }
        Registrar faultOwner = new Registrar(); AtomicBoolean armed = new AtomicBoolean();
        RuntimeException hashFailure = new RuntimeException("native-original-hash");
        NonNullConsumer<Item> callback = new NonNullConsumer<>() {
            public void accept(Item value) {}
            public int hashCode() { if (armed.get()) throw hashFailure; return 17; }
        };
        faultOwner.addRegisterCallback("fault", Registries.ITEM, callback); armed.set(true);
        try { faultOwner.capture("fault", () -> {}); throw new AssertionError("Missing original critical-slice failure"); }
        catch (RuntimeException actual) { require(actual == hashFailure, "Original critical exception identity changed"); }
        Thread after = thread("registrate-after-exception-reader", failure, () -> { faultOwner.getAll(Registries.ITEM); faultOwner.getOptional("absent", Registries.ITEM); });
        after.start(); try { join(after); noFailure(failure); } finally { cleanup(after); }
        require(owner.creators.get() == 0 && faultOwner.creators.get() == 0, "Creator ran during capture-only tests");
        witness("factory-exception"); helper.succeed();
    }
}
