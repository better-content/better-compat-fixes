package com.bettercontent.bettercompatfixes.nativewitness;

import com.bettercontent.bettercompatfixes.nativewitness.RootySoilLedger;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RootySoilLedgerTest {
    private static final RootySoilLedger.Pos POS = new RootySoilLedger.Pos(3, 62, -8);
    private static final RootySoilLedger.Generation GEN = new RootySoilLedger.Generation(POS, POS, 5, 2, false);
    private static final List<RootySoilLedger.Caller> CALLERS = List.of(new RootySoilLedger.Caller("native.JoCode", "generate", 271));
    private static void warn(RootySoilLedger ledger) {
        ledger.unsupported("dynamictrees:oak", "minecraft:seagrass", POS, 0, 179, "worldgen", CALLERS);
    }
    private static void set(RootySoilLedger ledger, boolean returned) {
        var ticket = ledger.setStart(new RootySoilLedger.SetArgs(POS, "dynamictrees:rooty_dirt", 302, 3));
        ledger.setReturn(ticket, returned);
    }
    private static void row(RootySoilLedger ledger) {
        var call = ledger.begin(GEN); warn(ledger); set(ledger, true); ledger.finish(call, false, null);
    }
    @Test void actualFalseReturnAndSetArgumentsNotRelabeled() {
        var ledger = new RootySoilLedger(); var call = ledger.begin(GEN);
        warn(ledger); set(ledger, false); ledger.finish(call, false, null);
        String report = ledger.sealAndRender();
        assertTrue(report.contains("\"observerComplete\":true"));
        assertTrue(report.contains("\"block\":\"dynamictrees:rooty_dirt\",\"stateId\":302,\"flags\":3,\"returned\":false"));
        assertTrue(report.contains("\"soil\":\"minecraft:seagrass\",\"root\":[3,62,-8]"));
        assertTrue(report.contains("\"fullLogReconciliationRequired\":true"));
    }
    @Test void ordinaryCallsProduceNoDiagnosticRowsOrGuessedGenerations() {
        var ledger = new RootySoilLedger(); var call = ledger.begin(null); ledger.finish(call, true, null);
        assertTrue(ledger.sealAndRender().contains("\"observerComplete\":true"));
    }
    @Test void missingGenerationAndLocalsRejectCompletenessWithoutPayloadLeak() {
        var ledger = new RootySoilLedger(); var call = ledger.begin(null);
        ledger.unsupported("private\nPLAYER_NBT_SECRET", null, null, 0, -1, null,
                List.of(new RootySoilLedger.Caller("secret\nNBT", "generate", -1)));
        set(ledger, true); ledger.finish(call, false, null);
        String report = ledger.sealAndRender();
        assertTrue(report.contains("\"observerComplete\":false"));
        assertFalse(report.contains("PLAYER_NBT_SECRET")); assertFalse(report.contains("secret"));
    }
    @Test void missingActualSetCannotPass() {
        var ledger = new RootySoilLedger(); var call = ledger.begin(GEN); warn(ledger); ledger.finish(call, false, null);
        assertTrue(ledger.sealAndRender().contains("\"observerComplete\":false"));
    }
    @Test void unexpectedTrueReturnCannotPass() {
        var ledger = new RootySoilLedger(); var call = ledger.begin(GEN); warn(ledger); set(ledger, true); ledger.finish(call, true, null);
        assertTrue(ledger.sealAndRender().contains("\"observerComplete\":false"));
    }
    @Test void doubleForwardAndDoubleFinishDetected() {
        var ledger = new RootySoilLedger(); var call = ledger.begin(GEN); warn(ledger); set(ledger, true); set(ledger, true);
        ledger.finish(call, false, null); ledger.finish(call, false, null);
        assertTrue(ledger.sealAndRender().contains("\"doubleForward\":2"));
    }
    @Test void recordOverflowRetainsFirstAndRejectsCompleteness() {
        var ledger = new RootySoilLedger(1, RootySoilLedger.MAX_BYTES); row(ledger); row(ledger);
        String report = ledger.sealAndRender();
        assertTrue(report.contains("\"dropped\":1")); assertTrue(report.contains("\"observerComplete\":false"));
    }
    @Test void byteBudgetIncludesReservedMetadataSpace() {
        var ledger = new RootySoilLedger(128, 2048); row(ledger);
        String report = ledger.sealAndRender();
        assertTrue(report.length() <= 2048); assertTrue(report.contains("\"dropped\":1"));
    }
    @Test void openFrameAndMissingForwardReject() {
        var ledger = new RootySoilLedger(); ledger.begin(GEN); warn(ledger);
        String report = ledger.sealAndRender();
        assertTrue(report.contains("\"open\":1")); assertTrue(report.contains("\"observerComplete\":false"));
    }
    @Test void nestedFramesDoNotCrossAttribute() {
        var ledger = new RootySoilLedger(); var outer = ledger.begin(GEN); warn(ledger);
        var inner = ledger.begin(new RootySoilLedger.Generation(POS, POS, 9, 4, true));
        warn(ledger); set(ledger, true); ledger.finish(inner, false, null);
        set(ledger, false); ledger.finish(outer, false, null);
        String report = ledger.sealAndRender();
        assertTrue(report.contains("\"observerComplete\":true"));
        assertTrue(report.contains("\"radius\":9,\"facing\":4,\"secondChance\":true"));
        assertTrue(report.contains("\"radius\":5,\"facing\":2,\"secondChance\":false"));
    }
    @Test void exceptionAndLateObservationCannotBeAccepted() {
        var ledger = new RootySoilLedger(); var call = ledger.begin(GEN); warn(ledger);
        var set = ledger.setStart(new RootySoilLedger.SetArgs(POS, "dynamictrees:rooty_dirt", 302, 3));
        ledger.setThrown(set, "java.lang.IllegalStateException"); ledger.finish(call, null, "java.lang.IllegalStateException");
        assertTrue(ledger.sealAndRender().contains("\"observerComplete\":false"));
        var late = ledger.begin(GEN); ledger.finish(late, true, null);
        assertTrue(ledger.sealAndRender().contains("\"late\":1"));
    }
    @Test void depthOverflowIsBoundedAndIncomplete() {
        var ledger = new RootySoilLedger();
        RootySoilLedger.Call[] calls = new RootySoilLedger.Call[RootySoilLedger.MAX_DEPTH + 1];
        for (int i = 0; i < calls.length; i++) calls[i] = ledger.begin(GEN);
        for (int i = calls.length - 1; i >= 0; i--) ledger.finish(calls[i], true, null);
        String report = ledger.sealAndRender();
        assertTrue(report.contains("\"overflow\":1")); assertTrue(report.contains("\"open\":0"));
    }
}
