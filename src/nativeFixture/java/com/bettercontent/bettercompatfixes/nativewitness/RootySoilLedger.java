package com.bettercontent.bettercompatfixes.nativewitness;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Nonshipping diagnostic ledger. Only bounded public identifiers and scalar snapshots enter it. */
public final class RootySoilLedger {
    public static final int MAX_RECORDS = 128, MAX_BYTES = 131072, MAX_DEPTH = 16, MAX_CALLERS = 8;
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern SYMBOL = Pattern.compile("[A-Za-z0-9_.$<>/-]+");
    public record Pos(int x, int y, int z) { }
    public record Generation(Pos origin, Pos root, int radius, int facing, boolean secondChance) { }
    public record Caller(String owner, String method, int line) { }
    public record SetArgs(Pos pos, String block, int stateId, int flags) { }
    public static final class Call {
        private final Generation generation;
        private String species, soil, thread, exception;
        private Pos root;
        private int fertility, soilStateId, warnings, sets;
        private List<Caller> callers = List.of();
        private SetArgs set;
        private Boolean setResult, returned;
        private boolean finished;
        private Call(Generation generation) { this.generation = generation; }
    }

    private final int maxRecords, maxBytes;
    private final ThreadLocal<ArrayDeque<Call>> stack = ThreadLocal.withInitial(ArrayDeque::new);
    private final ThreadLocal<Integer> suppressed = ThreadLocal.withInitial(() -> 0);
    private final List<String> rows = new ArrayList<>();
    private long calls, finished, open, unsupported, dropped, overflow, missing, doubleForward, late, exceptions;
    private int bytes;
    private boolean sealed;

    public RootySoilLedger() { this(MAX_RECORDS, MAX_BYTES); }
    public RootySoilLedger(int maxRecords, int maxBytes) {
        if (maxRecords < 1 || maxRecords > MAX_RECORDS || maxBytes < 2048 || maxBytes > MAX_BYTES)
            throw new IllegalArgumentException("Witness bounds");
        this.maxRecords = maxRecords; this.maxBytes = maxBytes;
    }
    public synchronized Call begin(Generation generation) {
        calls++;
        if (sealed) late++;
        if (stack.get().size() >= MAX_DEPTH || suppressed.get() > 0) {
            overflow++; suppressed.set(suppressed.get() + 1); return null;
        }
        Call call = new Call(generation);
        stack.get().push(call); open++;
        return call;
    }
    private Call current() { return suppressed.get() > 0 ? null : stack.get().peek(); }
    public synchronized void unsupported(String species, String soil, Pos root, int fertility,
                                         int soilStateId, String thread, List<Caller> callers) {
        unsupported++;
        Call call = current();
        if (call == null) { missing++; return; }
        if (++call.warnings != 1) doubleForward++;
        call.species = identifier(species); call.soil = identifier(soil);
        call.root = root; call.fertility = fertility; call.soilStateId = soilStateId;
        if (thread == null) { missing++; thread = "other"; }
        call.thread = switch (thread) { case "server", "worldgen", "other" -> thread; default -> "other"; };
        if (root == null || call.generation == null || call.generation.origin() == null || call.generation.root() == null) missing++;
        if (callers == null || callers.isEmpty() || callers.size() > MAX_CALLERS) { missing++; return; }
        List<Caller> safe = new ArrayList<>();
        for (Caller caller : callers) {
            if (caller == null) { missing++; continue; }
            safe.add(new Caller(symbol(caller.owner()), symbol(caller.method()), caller.line()));
        }
        call.callers = List.copyOf(safe);
    }
    public synchronized boolean awaitingSet() { Call call = current(); return call != null && call.warnings > 0; }
    public synchronized void observerFailure() { missing++; }
    /** Returns a ticket only for the unsupported branch; all native calls still run regardless. */
    public synchronized Call setStart(SetArgs args) {
        Call call = current();
        if (call == null || call.warnings == 0) return null;
        if (++call.sets != 1) doubleForward++;
        if (args == null || args.pos() == null) { missing++; return call; }
        call.set = new SetArgs(args.pos(), identifier(args.block()), args.stateId(), args.flags());
        return call;
    }
    public synchronized void setReturn(Call call, boolean returned) {
        if (call != null) call.setResult = returned;
    }
    public synchronized void setThrown(Call call, String exceptionClass) {
        if (call != null) { call.exception = symbol(exceptionClass); exceptions++; }
    }
    public synchronized void finish(Call call, Boolean returned, String exceptionClass) {
        if (call == null) {
            int depth = suppressed.get();
            if (depth == 0) { missing++; return; }
            if (depth == 1) suppressed.remove(); else suppressed.set(depth - 1);
            finished++; return;
        }
        if (call.finished || stack.get().peek() != call) { doubleForward++; return; }
        call.finished = true; stack.get().pop();
        if (stack.get().isEmpty()) stack.remove();
        open--; finished++; call.returned = returned;
        if (exceptionClass != null) { call.exception = symbol(exceptionClass); exceptions++; }
        if (call.warnings == 0) return;
        if (call.warnings != 1 || call.sets != 1 || call.set == null || call.setResult == null
                || !Boolean.FALSE.equals(returned) || call.exception != null) missing++;
        String row = row(call);
        // Public strings are ASCII-validated; character count equals UTF-8 bytes.
        if (sealed || rows.size() >= maxRecords || bytes + row.length() + 1 > maxBytes - 2048) {
            dropped++; overflow++; return;
        }
        rows.add(row); bytes += row.length() + 1;
    }
    private String identifier(String id) {
        if (id != null && id.length() <= 128 && ID.matcher(id).matches()) return id;
        missing++; return "witness:missing";
    }
    private String symbol(String value) {
        if (value != null && !value.isEmpty() && value.length() <= 128 && SYMBOL.matcher(value).matches()) return value;
        missing++; return "missing";
    }
    private static String pos(Pos p) { return p == null ? "null" : "[" + p.x() + "," + p.y() + "," + p.z() + "]"; }
    private static String row(Call c) {
        Generation g = c.generation;
        String gen = g == null ? "null" : "{\"origin\":" + pos(g.origin()) + ",\"root\":" + pos(g.root())
                + ",\"radius\":" + g.radius() + ",\"facing\":" + g.facing() + ",\"secondChance\":" + g.secondChance() + "}";
        StringBuilder callers = new StringBuilder("[");
        for (Caller caller : c.callers) {
            if (callers.length() > 1) callers.append(',');
            callers.append("{\"class\":\"").append(caller.owner()).append("\",\"method\":\"")
                    .append(caller.method()).append("\",\"line\":").append(caller.line()).append('}');
        }
        SetArgs s = c.set;
        return "{\"species\":\"" + c.species + "\",\"soil\":\"" + c.soil + "\",\"root\":" + pos(c.root)
                + ",\"fertility\":" + c.fertility + ",\"soilStateId\":" + c.soilStateId + ",\"generation\":" + gen
                + ",\"thread\":\"" + c.thread + "\",\"callerPrefix\":" + callers.append(']')
                + ",\"warnings\":" + c.warnings + ",\"setCalls\":" + c.sets + ",\"setBlock\":"
                + (s == null ? "null" : "{\"pos\":" + pos(s.pos()) + ",\"block\":\"" + s.block()
                + "\",\"stateId\":" + s.stateId() + ",\"flags\":" + s.flags() + ",\"returned\":" + c.setResult + "}")
                + ",\"returned\":" + c.returned + ",\"exception\":"
                + (c.exception == null ? "null" : "\"" + c.exception + "\"") + "}";
    }
    /** Completeness is observer-local; a separate after-JVM verifier must reconcile complete DEBUG/latest WARNs. */
    public synchronized String sealAndRender() {
        sealed = true;
        boolean complete = open == 0 && calls == finished && unsupported == rows.size() && missing == 0
                && overflow == 0 && doubleForward == 0 && late == 0 && exceptions == 0;
        return "{\"schema\":\"bc.native.rooty-soil-witness.v1\",\"observerComplete\":" + complete
                + ",\"fullLogReconciliationRequired\":true,\"callerPrefixLimit\":" + MAX_CALLERS
                + ",\"recordLimit\":" + maxRecords + ",\"byteLimit\":" + maxBytes + ",\"recordBytes\":" + bytes
                + ",\"calls\":" + calls + ",\"finished\":" + finished + ",\"open\":" + open
                + ",\"unsupported\":" + unsupported + ",\"dropped\":" + dropped + ",\"overflow\":" + overflow
                + ",\"missing\":" + missing + ",\"doubleForward\":" + doubleForward + ",\"late\":" + late
                + ",\"exceptions\":" + exceptions + ",\"records\":[" + String.join(",", rows) + "]}\n";
    }
}
