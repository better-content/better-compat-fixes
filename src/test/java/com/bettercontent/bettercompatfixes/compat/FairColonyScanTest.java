package com.bettercontent.bettercompatfixes.compat;

import com.bettercontent.bettercompatfixes.compat.bees.FairColonyScan;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class FairColonyScanTest {
    @Test void unrelatedDenseEarlyChunksCannotPermanentlyStarveLateColonies() {
        FairColonyScan<Integer, Integer> scan = new FairColonyScan<>();
        Map<Integer, Iterable<Integer>> chunks = new LinkedHashMap<>();
        chunks.put(0, IntStream.range(0, 2000).boxed().toList());
        chunks.put(1, List.of(9999));
        assertEquals(List.of(9999), scan.scan(chunks, 512, v -> v == 9999, 8));
        chunks = Map.of(0, IntStream.range(0, 2001).boxed().toList());
        boolean seen = false;
        for (int i = 0; i < 6; i++) seen |= !scan.scan(chunks, 512, v -> v == 2000, 8).isEmpty();
        assertTrue(seen, "Cursor must continue past 512 unrelated entities in one chunk");
    }
    @Test void moreThan512ColoniesAllGetAnEmissionWithoutBatchRotationAliasing() {
        FairColonyScan<Integer, Integer> scan = new FairColonyScan<>();
        Map<Integer, Iterable<Integer>> chunks = new LinkedHashMap<>();
        chunks.put(0, IntStream.range(0, 1024).boxed().toList());
        chunks.put(1, IntStream.range(1024, 2048).boxed().toList());
        Set<Integer> emitted = new HashSet<>();
        for (int i = 0; i < 256; i++) {
            var batch = scan.scan(chunks, 512, v -> true, 8);
            assertEquals(8, batch.size());
            for (int value : batch) assertTrue(emitted.add(value), "Must not discard scanned eligible sources past emission cap");
        }
        assertEquals(2048, emitted.size());
    }
    @Test void emptyUnloadedReplacedAndMutableChunkCollectionsRemainBounded() {
        FairColonyScan<Integer, Integer> scan = new FairColonyScan<>();
        List<Integer> values = new ArrayList<>(List.of(1, 2, 3));
        assertEquals(List.of(1), scan.scan(Map.of(0, values), 1));
        values.add(4); // Native membership change invalidates just this iterator.
        assertEquals(List.of(), scan.scan(Map.of(0, values), 1));
        assertEquals(List.of(1, 2, 3, 4), scan.scan(Map.of(0, values), 512));
        assertEquals(List.of(), scan.scan(Map.of(), 512));
        assertEquals(List.of(8), scan.scan(Map.of(0, List.of(8)), 512));
        assertEquals(List.of(), scan.scan(Map.of(0, List.of(8)), 0));
        scan.clear();
        assertEquals(List.of(1), scan.scan(Map.of(0, values), 1));
    }
}
