package com.minetopia.village.storage;

/**
 * Result of scanning the interior of a HOUSE structure.
 *
 * @param floorSpace  number of positions where a villager can stand
 *                    (solid block below, passable self, passable above) — counted
 *                    across all floors so multi-storey buildings score higher
 * @param bedCount    number of beds inside (each bed counted once)
 */
public record HouseScanResult(int floorSpace, int bedCount) {

    /** Fraction of floor space taken by beds. 0 if no floor space was found. */
    public double crowdingRatio() {
        return floorSpace == 0 ? 0.0 : (double) bedCount / floorSpace;
    }
}
