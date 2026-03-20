package com.minetopia.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-village economy state: tracks recent trade counts per trade key,
 * recomputes demand values every {@link #REFRESH_TICKS} ticks.
 *
 * <p>Demand is a signed integer fed directly into {@link net.minecraft.world.item.trading.MerchantOffer}.
 * Positive demand raises price; negative demand lowers it. We cap at ±5 to keep
 * prices within a reasonable band (NeoForge's price formula multiplies demand by
 * priceMultiplier, so demand=5 at priceMultiplier=0.05 adds 25% to the cost).</p>
 */
public class VillageEconomy {

    public static final int REFRESH_TICKS = 4000;
    private static final int MAX_HISTORY  = 50;
    private static final int DEMAND_CAP   = 5;

    public static final Codec<VillageEconomy> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .fieldOf("trade_counts").forGetter(e -> e.tradeCounts),
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .fieldOf("demand_values").forGetter(e -> e.demandValues)
            ).apply(instance, VillageEconomy::new)
    );

    /** Rolling count of completed trades since the last refresh, keyed by ProfessionTrade.key(). */
    private final Map<String, Integer> tradeCounts;
    /** Current demand per trade key — fed into MerchantOffer on rebuild. */
    private final Map<String, Integer> demandValues;

    private int tickTimer = 0;

    public VillageEconomy() {
        this(new HashMap<>(), new HashMap<>());
    }

    private VillageEconomy(Map<String, Integer> tradeCounts, Map<String, Integer> demandValues) {
        this.tradeCounts  = new HashMap<>(tradeCounts);
        this.demandValues = new HashMap<>(demandValues);
    }

    // --- Tick / refresh ---

    /**
     * Called every level tick. Returns true when demand values were refreshed
     * (so callers can rebuild their MerchantOffers).
     */
    public boolean tick() {
        if (++tickTimer < REFRESH_TICKS) return false;
        tickTimer = 0;
        refreshDemand();
        tradeCounts.clear();
        return true;
    }

    private void refreshDemand() {
        // For each tracked trade: lots of sales → demand up → price up;
        // few/no sales → demand down → price down. No-history trades drift toward 0.
        for (Map.Entry<String, Integer> entry : tradeCounts.entrySet()) {
            String key = entry.getKey();
            int count  = entry.getValue();
            int current = demandValues.getOrDefault(key, 0);
            // Normalise against history cap: >50% of max = demand up, <50% = down
            int threshold = MAX_HISTORY / 2;
            int delta = count > threshold ? 1 : (count < threshold / 2 ? -1 : 0);
            demandValues.put(key, Math.clamp(current + delta, -DEMAND_CAP, DEMAND_CAP));
        }
        // Trades not seen this period drift toward 0
        for (String key : demandValues.keySet()) {
            if (!tradeCounts.containsKey(key)) {
                int v = demandValues.get(key);
                if (v != 0) demandValues.put(key, v > 0 ? v - 1 : v + 1);
            }
        }
    }

    // --- Called when a trade executes ---

    public void recordTrade(String tradeKey) {
        tradeCounts.merge(tradeKey, 1, (a, b) -> Math.min(a + b, MAX_HISTORY));
    }

    // --- Query ---

    public int getDemand(String tradeKey) {
        return demandValues.getOrDefault(tradeKey, 0);
    }

    /** Returns an unmodifiable snapshot of all current demand values, keyed by trade key. */
    public java.util.Map<String, Integer> getDemandSnapshot() {
        return java.util.Collections.unmodifiableMap(demandValues);
    }
}
