package com.bettercontent.bettercompatfixes.compat.bees;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Bounded round-robin over live loaded-chunk collections, retaining cursors rather than snapshots. */
public final class FairColonyScan<K, T> {
    private final Map<K, Cursor<T>> cursors = new HashMap<>();
    private int next;
    private static final class Cursor<T> {
        final Iterable<T> source;
        Iterator<T> iterator;
        boolean exhausted;
        Cursor(Iterable<T> source) { this.source = source; }
    }

    public void clear() { cursors.clear(); next = 0; }

    public List<T> scan(Map<K, ? extends Iterable<T>> loaded, int budget) {
        return scan(loaded, budget, value -> true, Integer.MAX_VALUE);
    }

    public List<T> scan(Map<K, ? extends Iterable<T>> loaded, int budget,
            java.util.function.Predicate<T> eligible, int resultLimit) {
        cursors.keySet().retainAll(loaded.keySet()); // never retain an unloaded chunk's iterator
        List<K> keys = new ArrayList<>(loaded.keySet());
        for (K key : keys) {
            Iterable<T> source = loaded.get(key);
            Cursor<T> cursor = cursors.get(key);
            if (cursor == null || cursor.source != source) {
                cursor = new Cursor<>(source);
                cursors.put(key, cursor);
            }
            cursor.exhausted = false;
            if (cursor.iterator == null) cursor.iterator = source.iterator();
        }
        List<T> values = new ArrayList<>();
        int exhausted = 0;
        // Every iterator operation, including empty/restarted collections, spends budget.
        for (int attempts = 0; attempts < budget && exhausted < keys.size() && values.size() < resultLimit; attempts++) {
            Cursor<T> cursor = cursors.get(keys.get(Math.floorMod(next++, keys.size())));
            if (cursor.exhausted) continue;
            try {
                if (cursor.iterator.hasNext()) {
                    T value = cursor.iterator.next();
                    if (eligible.test(value)) values.add(value);
                }
                else { cursor.iterator = null; cursor.exhausted = true; exhausted++; }
            } catch (ConcurrentModificationException changed) {
                // Native block-entity membership changed between scans; restart only this collection.
                cursor.iterator = null;
                cursor.exhausted = true;
                exhausted++;
            }
        }
        return values;
    }
}
