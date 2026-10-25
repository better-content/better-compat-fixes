package com.bettercontent.bettercompatfixes.compat.eanimod;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class VillageAnimalPoolMutabilityTest {
    @Test void onlyTheReviewedOptionalProviderVersionIsAdmitted() {
        assertTrue(VillageAnimalPoolMutability.applies("0.11.12"));
        for (String other : new String[]{null, "", "0.11.11", "0.11.13", "0.11.12-unreviewed"})
            assertFalse(VillageAnimalPoolMutability.applies(other));
    }
    @Test void immutableCopyRetainsOrderIdentityAndMultiplicityWithoutMutatingInput() {
        Object a = new Object(), b = new Object();
        var input = ImmutableList.of(a, b, a);
        var output = VillageAnimalPoolMutability.mutableIfKnownImmutable(input);
        assertEquals(List.of(a, b, a), output);
        assertNotSame(input, output);
        assertSame(a, output.get(0));
        assertSame(b, output.get(1));
        output.add(b);
        assertEquals(List.of(a, b, a), input);
        assertEquals(List.of(a, b, a, b), output);
        assertSame(output, VillageAnimalPoolMutability.mutableIfKnownImmutable(output));
    }
    @Test void mutableProducerIsNotReplacedAndEmptyImmutableProducerCanBeAppended() {
        var input = new ArrayList<>(List.of("existing"));
        assertSame(input, VillageAnimalPoolMutability.mutableIfKnownImmutable(input));
        var empty = VillageAnimalPoolMutability.mutableIfKnownImmutable(ImmutableList.<String>of());
        empty.add("native addition");
        assertEquals(List.of("native addition"), empty);
    }
}
