#!/usr/bin/env python3
"""JDK-only exact Mixin selector proof; never loads Minecraft or launches Gradle.

Explicit raw pinned inputs are required. All generated proof files are retained in
one fresh directory below --evidence-root (no temporary-directory cleanup).
This checks selector resolution, NOT transformed/native emission behavior.
"""
from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import subprocess
import tempfile
import zipfile

MIXIN_SHA256 = "ca15a907e4d1f6b38cefed51d7df96a83347dcfb0bc8ced53111519e991d887d"
HEARTH_CLASS = "com/momosoftworks/coldsweat/common/blockentity/HearthBlockEntity.class"
HEARTH_SHA256 = "455c9ecbfe05585b898cef5ac98ffe42222453793850717f88593c908af95859"
JAVA = r'''
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import com.bettercontent.bettercompatfixes.nativewitness.fuel.FuelEmissionWitness;

public class HearthSelectorProof {
    static final String OWNER = "com/momosoftworks/coldsweat/common/blockentity/HearthBlockEntity";
    static final String INNER = "(L" + OWNER + "$FuelType;IZ)V";
    static MemberInfo selector(String text) {
        return (MemberInfo) MemberInfo.parse(text, null)
            .configure(ITargetSelector.Configure.SELECT_MEMBER);
    }
    // Exact relevant findRootTargets traversal: exact match, then stop at max.
    // Target nodes here are original upstream methods, none MixinMerged/static.
    static List<String> selected(MemberInfo selector, List<MethodNode> methods) {
        List<String> result = new ArrayList<>();
        for (MethodNode method : methods) {
            if (selector.matches(OWNER, method.name, method.desc).isExactMatch()) {
                result.add(method.name + method.desc);
                if (result.size() >= selector.getMaxMatchCount()) break;
            }
        }
        return result;
    }
    static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        System.out.println("PASS " + label);
    }
    public static void main(String[] args) throws Exception {
        ClassNode node = new ClassNode();
        try (ZipFile jar = new ZipFile(args[0])) {
            new ClassReader(jar.getInputStream(jar.getEntry(OWNER + ".class")))
                .accept(node, 0);
        }
        MemberInfo loose = selector("drainFuel"), exact = selector("drainFuel()V");
        check(loose.getMaxMatchCount() == 1, "actual Mixin0.8.5 SELECT_MEMBER maximum is one");
        check(loose.matches(OWNER, "drainFuel", "()V").isExactMatch()
            && loose.matches(OWNER, "drainFuel", INNER).isExactMatch(),
            "unqualified selector matches both overload descriptors");
        check(selected(loose, node.methods).equals(List.of("drainFuel()V")),
            "actual original method order selects only outer automatic drain");
        List<MethodNode> reversed = new ArrayList<>(node.methods);
        Collections.reverse(reversed);
        check(selected(loose, reversed).equals(List.of("drainFuel" + INNER)),
            "reordered loose selector selects wrong manual drain overload");
        check(selected(exact, node.methods).equals(List.of("drainFuel()V"))
            && selected(exact, reversed).equals(List.of("drainFuel()V")),
            "exact descriptor selects outer in either class method order");
        check(!exact.matches(OWNER, "drainFuel", INNER).isExactMatch(),
            "exact descriptor excludes nested manual drain overload");
        List<MethodNode> nestedOnly = node.methods.stream()
            .filter(m -> !m.name.equals("drainFuel") || !m.desc.equals("()V")).toList();
        check(selected(exact, nestedOnly).isEmpty(),
            "missing outer cannot silently fall back to manual drain");
        MethodNode outer = node.methods.stream()
            .filter(m -> m.name.equals("drainFuel") && m.desc.equals("()V")).findFirst().orElseThrow();
        check(Arrays.stream(outer.instructions.toArray()).anyMatch(i ->
            i instanceof MethodInsnNode call && call.owner.equals(OWNER)
                && call.name.equals("drainHotFuel") && call.desc.equals("(IZ)V")),
            "actual outer automatic drain delegates hot-fuel consumption");
        MethodNode hot = node.methods.stream()
            .filter(m -> m.name.equals("drainHotFuel") && m.desc.equals("(IZ)V")).findFirst().orElseThrow();
        check(Arrays.stream(hot.instructions.toArray()).anyMatch(i ->
            i instanceof MethodInsnNode call && call.owner.equals(OWNER)
                && call.name.equals("drainFuel") && call.desc.equals(INNER)),
            "actual hot drain delegates nested parameterized overload");
        Object pollutant = new Object();
        var one = FuelEmissionWitness.observe(() -> {
            FuelEmissionWitness.amount(pollutant, 1, 0.9, 0.15);
            FuelEmissionWitness.completed();
        });
        var carried = FuelEmissionWitness.observe(() -> {
            FuelEmissionWitness.amount(pollutant, 0, 0.1, 0.35);
            FuelEmissionWitness.completed();
        });
        check(one.positive() && one.equivalent(carried), "witness reconstructs real emission across fractional carry");
        var twice = FuelEmissionWitness.observe(() -> {
            for (int i = 0; i < 2; i++) {
                FuelEmissionWitness.amount(pollutant, 0, 0, 0.25);
                FuelEmissionWitness.completed();
            }
        });
        check(!one.equivalent(twice) && twice.completed() == 2,
            "witness rejects genuinely doubled pollution operations");
        var zero = FuelEmissionWitness.observe(() -> {});
        check(!zero.positive() && zero.completed() == 0 && zero.amounts().isEmpty()
            && !one.equivalent(zero), "zero cannot masquerade as positive native pollution");
        var noPollution = FuelEmissionWitness.observe(FuelEmissionWitness::completed);
        check(!noPollution.positive() && !one.equivalent(noPollution),
            "completed call without effective pollution fails positive proof");
        try {
            FuelEmissionWitness.observe(() -> { throw new IllegalStateException("fault control"); });
            throw new AssertionError("fault must propagate");
        } catch (IllegalStateException expected) {
            check(FuelEmissionWitness.observe(() -> {}).completed() == 0,
                "operation failure propagates and observation scope is released");
        }
        FuelEmissionWitness.amount(pollutant, 99, 0, 0);
        FuelEmissionWitness.completed();
        check(FuelEmissionWitness.observe(() -> {}).amounts().isEmpty(),
            "unscoped unrelated operations do not contaminate samples");
    }
}
'''


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--libraries", type=Path, required=True)
    parser.add_argument("--hearth-jar", type=Path, required=True)
    parser.add_argument("--evidence-root", type=Path, required=True)
    args = parser.parse_args()
    jars = sorted(args.libraries.resolve().rglob("*.jar"))
    mixins = [p for p in jars if p.name == "mixin-0.8.5.jar"]
    if len(mixins) != 1 or hashlib.sha256(mixins[0].read_bytes()).hexdigest() != MIXIN_SHA256:
        raise SystemExit("Exact pinned Mixin0.8.5 library required")
    with zipfile.ZipFile(args.hearth_jar) as jar:
        if jar.namelist().count(HEARTH_CLASS) != 1:
            raise SystemExit("Missing/duplicate original Hearth class")
        if hashlib.sha256(jar.read(HEARTH_CLASS)).hexdigest() != HEARTH_SHA256:
            raise SystemExit("Exact pinned ColdSweat2.4 Hearth class required")
    source = Path(__file__).resolve().parents[1] / (
        "src/main/java/com/bettercontent/bettercompatfixes/mixin/adpother/coldsweat/HearthBlockEntityMixin.java")
    text = source.read_text()
    if text.count('@Inject(method = "drainFuel()V"') != 2 or '@Inject(method = "drainFuel"' in text:
        raise SystemExit("Both production injection selectors must be exact drainFuel()V")
    print("PASS both production selectors use exact outer descriptor")
    args.evidence_root.mkdir(parents=True, exist_ok=True)
    out = Path(tempfile.mkdtemp(prefix="hearth-selector-", dir=args.evidence_root))
    java = out / "HearthSelectorProof.java"
    java.write_text(JAVA)
    cp = ":".join(str(p) for p in jars)
    witness = Path(__file__).resolve().parents[1] / (
        "src/nativeFixture/java/com/bettercontent/bettercompatfixes/nativewitness/fuel/FuelEmissionWitness.java")
    commands = [
        ["javac", "-proc:none", "-cp", cp, "-d", str(out), str(java), str(witness)],
        ["java", "-Djava.io.tmpdir=" + str(out), "-cp", str(out) + ":" + cp,
         "HearthSelectorProof", str(args.hearth_jar.resolve())],
    ]
    for index, command in enumerate(commands):
        result = subprocess.run(command, capture_output=True, text=True, check=False)
        (out / f"command-{index}.txt").write_text(result.stdout + result.stderr)
        print(result.stdout, end="")
        if result.returncode:
            raise SystemExit(f"JDK proof failed; retained evidence: {out}\n{result.stderr}")
    print(f"Retained proof: {out}")


if __name__ == "__main__":
    main()
