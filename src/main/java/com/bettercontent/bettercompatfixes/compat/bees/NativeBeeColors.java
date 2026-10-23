package com.bettercontent.bettercompatfixes.compat.bees;

import com.accbdd.complicated_bees.bees.Chromosome;
import com.accbdd.complicated_bees.bees.Species;
import com.accbdd.complicated_bees.bees.gene.GeneSpecies;
import java.util.HashMap;

/** Optional-mod-only, pinned direct-field getters. Never decode genes or query inventories/worlds. */
public final class NativeBeeColors {
    private NativeBeeColors() { }
    public static DecodedColorScope.Sample fromSpecies(Species species) {
        if (species == null || species == Species.INVALID || species.getClass() != Species.class)
            return DecodedColorScope.Sample.UNKNOWN;
        int rgb = species.getColor();
        return rgb >= 0 && rgb <= 0xffffff ? new DecodedColorScope.Sample(true, rgb) : DecodedColorScope.Sample.UNKNOWN;
    }
    public static DecodedColorScope.Sample fromDecoded(Chromosome chromosome) {
        if (chromosome == null || chromosome.getClass() != Chromosome.class) return DecodedColorScope.Sample.UNKNOWN;
        var genes = chromosome.getGenes();
        if (genes == null || genes.getClass() != HashMap.class) return DecodedColorScope.Sample.UNKNOWN;
        var gene = genes.get(GeneSpecies.ID);
        if (gene == null || gene.getClass() != GeneSpecies.class) return DecodedColorScope.Sample.UNKNOWN;
        Object value = gene.get();
        return value != null && value.getClass() == Species.class ? fromSpecies((Species) value) : DecodedColorScope.Sample.UNKNOWN;
    }
}
