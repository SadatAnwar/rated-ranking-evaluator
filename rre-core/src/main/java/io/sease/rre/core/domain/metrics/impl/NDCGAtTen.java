package io.sease.rre.core.domain.metrics.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.sease.rre.core.domain.metrics.Metric;
import io.sease.rre.core.domain.metrics.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;

public class NDCGAtTen extends Metric {
    /**
     * Builds a new NDCGAtTen metric.
     */
    public NDCGAtTen() {
        super("NDCG@10");
    }

    @Override
    public Value valueFactory() {
        return new Value(this) {
            private BigDecimal dcg = BigDecimal.ZERO;

            @Override
            public void collect(final Map<String, Object> hit, final int rank, final String version) {
                if (rank > 10) return;
                judgment(id(hit))
                        .ifPresent(judgment -> {
                            switch(rank) {
                                case 1:
                                    dcg = judgment.get("gain") == null || judgment.get("gain").isNull()
                                            ? new BigDecimal(2)
                                            : judgment.get("gain").decimalValue();
                                    break;
                                default:
                                    final double gain = judgment.get("gain").asDouble(2);
                                    dcg = dcg.add(new BigDecimal(gain / (Math.log(rank) / Math.log(2))));
                            }
                        });
            }
            @Override
            public BigDecimal value() {
                if (totalHits == 0) { return relevantDocuments.size() == 0 ? BigDecimal.ONE : BigDecimal.ZERO; }

                return dcg.divide(idealDcg(relevantDocuments), 2, RoundingMode.FLOOR);
            }
        };
    }

    private BigDecimal idealDcg(final JsonNode relevantDocuments) {
        final int windowSize = Math.min(relevantDocuments.size(), 10); // TODO make it dynamic
        final int [] gains = new int [windowSize];

        final Map<Integer, List<JsonNode>> groups = StreamSupport.stream(relevantDocuments.spliterator(), false).collect(groupingBy(doc -> doc.get("gain").intValue()));

        final int veryVeryRelevantDocsCount= groups.getOrDefault(3, emptyList()).size();
        final int howManyVeryVeryRelevantDocs = Math.min(veryVeryRelevantDocsCount, windowSize);

        final int veryRelevantDocsCount= groups.getOrDefault(2, emptyList()).size();
        final int howManyVeryRelevantDocs = Math.min(veryRelevantDocsCount, windowSize - howManyVeryVeryRelevantDocs);

        final int marginallyRelevantDocsCount = groups.getOrDefault(1, emptyList()).size();
        final int howManyMarginallyRelevantDocs = Math.max(Math.min(marginallyRelevantDocsCount - howManyVeryRelevantDocs, 0), 0);

        Arrays.fill(gains, 0, howManyVeryVeryRelevantDocs, 3);
        if (howManyVeryVeryRelevantDocs < windowSize) {
            Arrays.fill(gains, howManyVeryVeryRelevantDocs, howManyVeryVeryRelevantDocs + Math.min((windowSize - howManyVeryVeryRelevantDocs), howManyVeryRelevantDocs), 1);
        }

        BigDecimal result = new BigDecimal(gains[0]);
        for (int i = 1 ; i < gains.length; i++) {
            result = result.add(new BigDecimal(gains[i] / (Math.log(i + 1) / Math.log(2))));
        }

        return result;
    }
}
