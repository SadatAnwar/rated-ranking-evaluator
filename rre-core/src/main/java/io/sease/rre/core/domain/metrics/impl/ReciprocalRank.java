package io.sease.rre.core.domain.metrics.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.sease.rre.core.domain.metrics.Metric;
import io.sease.rre.core.domain.metrics.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Reciprocal Rank metric
 *
 * @author agazzarini
 * @since 1.0
 */
public class ReciprocalRank extends Metric {
    /**
     * Builds a new ReciprocalRank at X metric.
     */
    public ReciprocalRank() {
        super("RR@10");
    }

    @Override
    public Value valueFactory() {
        return new Value(this) {
            private int rank;
            private int maxGain;

            @Override
            public void collect(final Map<String, Object> hit, final int rank, final String version) {
                judgment(id(hit)).ifPresent(hitData -> {
                    final int gain = hitData.get("gain").asInt();
                    if (gain > maxGain) {
                        this.rank = rank;
                        this.maxGain = gain;
                    }
                });
            }

            @Override
            public BigDecimal value() {
                if (totalHits == 0) { return relevantDocuments.size() == 0 ? BigDecimal.ONE : BigDecimal.ZERO; }
                if (rank == 0) { return BigDecimal.ZERO;}

                return BigDecimal.ONE.divide(new BigDecimal(rank), 2, RoundingMode.HALF_UP);
            }
        };
    }
}