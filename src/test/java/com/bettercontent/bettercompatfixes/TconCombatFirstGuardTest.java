package com.bettercontent.bettercompatfixes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs the compiled client guard handlers with progress doubles, not actual client rendering. */
public final class TconCombatFirstGuardTest {
    @Test
    public void startingBlockAttackIsCancelledOnlyForEligibleMainHand() throws Exception {
        Class<? extends Game> guard = guards();
        for (boolean eligible : new boolean[]{false, true}) for (Hit hit : new Hit[]{null, new Hit(Kind.BLOCK), new Hit(Kind.ENTITY)}) {
            Game game = guard.getConstructor().newInstance(); game.player.eligible = eligible; game.hitResult = hit;
            var callback = new CallbackInfoReturnable<Boolean>("startAttack", true, true);
            guard.getMethod("start", CallbackInfoReturnable.class).invoke(game, callback);
            boolean denied = eligible && hit != null && hit.getType() == Kind.BLOCK;
            assertEquals(denied, callback.isCancelled());
            assertEquals(denied ? 1 : 0, game.gameMode.stops);
            assertEquals(!denied, callback.getReturnValue());
        }
    }

    @Test
    public void heldAttackAlwaysResetsOldProgressWithoutNeedingAHitOrAPlayableAttack() throws Exception {
        Class<? extends Game> guard = guards();
        for (boolean eligible : new boolean[]{false, true}) for (boolean held : new boolean[]{false, true}) {
            Game game = guard.getConstructor().newInstance(); game.player.eligible = eligible;
            var callback = new CallbackInfo("continueAttack", true);
            guard.getMethod("continued", boolean.class, CallbackInfo.class).invoke(game, held, callback);
            assertEquals(eligible, callback.isCancelled());
            assertEquals(eligible ? 1 : 0, game.gameMode.stops);
            assertEquals(!eligible, game.gameMode.mining, "Tool swap must clear the old progress");
            // Native stopDestroyBlock has nothing to do without a game mode; still suppress block input.
            game.gameMode = null;
            callback = new CallbackInfo("continueAttack", true);
            guard.getMethod("continued", boolean.class, CallbackInfo.class).invoke(game, held, callback);
            assertEquals(eligible, callback.isCancelled());
        }
    }

    private static Class<? extends Game> guards() throws Exception {
        var source = new ClassNode();
        try (var input = TconCombatFirstGuardTest.class.getClassLoader().getResourceAsStream(
                "com/bettercontent/bettercompatfixes/mixin/tconcombat/CombatFirstMinecraftMixin.class")) {
            assertNotNull(input); new ClassReader(input).accept(source, 0);
        }
        var output = new ClassNode(); output.version = Opcodes.V17; output.access = Opcodes.ACC_PUBLIC;
        output.name = "com/bettercontent/bettercompatfixes/IsolatedBlockGuard"; output.superName = Type.getInternalName(Game.class);
        for (var method : source.methods) {
            if (method.name.contains("$neverStartBlockAttack")) method.name = "start";
            else if (method.name.contains("$neverContinueBlockAttack")) method.name = "continued";
            else continue;
            method.access = Opcodes.ACC_PUBLIC; output.methods.add(method);
        }
        assertEquals(2, output.methods.size());
        var ctor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ctor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, output.superName, "<init>", "()V", false));
        ctor.instructions.add(new InsnNode(Opcodes.RETURN)); output.methods.add(ctor);
        var types = Map.of("net/minecraft/client/Minecraft", Game.class,
                "net/minecraft/client/player/LocalPlayer", Player.class, "net/minecraft/world/entity/player/Player", Player.class,
                "net/minecraft/client/multiplayer/MultiPlayerGameMode", GameMode.class,
                "net/minecraft/world/phys/HitResult", Hit.class, "net/minecraft/world/phys/HitResult$Type", Kind.class,
                "com/bettercontent/bettercompatfixes/compat/TconCombatFirstPolicy", Policy.class);
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        output.accept(new ClassRemapper(writer, new Remapper() {
            @Override public String map(String type) { return types.containsKey(type) ? Type.getInternalName(types.get(type)) : type; }
        }));
        var bytes = writer.toByteArray();
        return new ClassLoader(TconCombatFirstGuardTest.class.getClassLoader()) {
            Class<?> emit() { return defineClass(null, bytes, 0, bytes.length); }
        }.emit().asSubclass(Game.class);
    }
    public static class Game { public Player player = new Player(); public Hit hitResult; public GameMode gameMode = new GameMode(); }
    public static class Player { public boolean eligible; }
    public static class Policy { public static boolean deniesBlockAttack(Player player) { return player != null && player.eligible; } }
    public static class GameMode { public int stops; public boolean mining = true; public void stopDestroyBlock() { stops++; mining = false; } }
    public enum Kind { BLOCK, ENTITY }
    public static class Hit { private final Kind kind; public Hit(Kind kind) { this.kind = kind; } public Kind getType() { return kind; } }
}
