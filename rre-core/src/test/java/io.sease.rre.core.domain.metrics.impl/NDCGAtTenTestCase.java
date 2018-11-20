package io.sease.rre.core.domain.metrics.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sease.rre.core.BaseTestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static io.sease.rre.core.TestData.*;
import static java.util.Arrays.stream;
import static org.junit.Assert.assertEquals;

public class NDCGAtTenTestCase extends BaseTestCase {

    @Before
    public void setUp() {
        cut = new NDCGAtTen();
        cut.setVersions(Collections.singletonList(A_VERSION));
        counter = new AtomicInteger(0);
    }

    /**
     * If all results in the window are relevant, then the NDCG is 1.
     */
    @Test
    public void maximumNdcg() {
       maximum();
    }

    /**
     * Scenario: 15 judgments, 15 search results, 10 relevant results in top positions. NDCG = 1
     */
    @Test
    public void _10_judgments_15_search_results_10_relevant_results_at_top() {
        final ObjectNode judgements = mapper.createObjectNode();
        stream(FIFTEEN_SEARCH_HITS).limit(10).forEach(docid -> judgements.set(docid, createJudgmentNode(3)));
        cut.setRelevantDocuments(judgements);

        cut.setTotalHits(FIFTEEN_SEARCH_HITS.length, A_VERSION);
        stream(FIFTEEN_SEARCH_HITS)
                .map(this::searchHit)
                .forEach(hit -> cut.collect(hit, counter.incrementAndGet(), A_VERSION));

        assertEquals(
                1,
                cut.valueFactory(A_VERSION).value().doubleValue(),
                0.001);
    }

    /**
     * Scenario: 10 judgments, 15 search results, 5 relevant results in top positions.
     */
    @Test
    public void _10_judgments_15_search_results_5_relevant_results_at_top() {
        final ObjectNode judgements = mapper.createObjectNode();
        stream(FIFTEEN_SEARCH_HITS).limit(10).forEach(docid -> judgements.set(docid, createJudgmentNode(3)));
        cut.setRelevantDocuments(judgements);

        cut.setTotalHits(FIFTEEN_SEARCH_HITS.length, A_VERSION);
        stream(FIVE_SEARCH_HITS)
                .map(this::searchHit)
                .forEach(hit -> cut.collect(hit, counter.incrementAndGet(), A_VERSION));

        stream(TEN_SEARCH_HITS)
                .map(docid -> docid + "_SUFFIX")
                .map(this::searchHit)
                .forEach(hit -> cut.collect(hit, counter.incrementAndGet(), A_VERSION));

        assertEquals(
                0.67,
                cut.valueFactory(A_VERSION).value().doubleValue(),
                0);
    }

    /**
     * Scenario: 10 judgments, 15 search results, 5 relevant results in top positions.
     */
    @Test
    public void _10_judgments_15_search_results_5_relevant_results_from_5th_to_10th() {
        final ObjectNode judgements = mapper.createObjectNode();
        stream(FIFTEEN_SEARCH_HITS).skip(5).forEach(docid -> judgements.set(docid, createJudgmentNode(3)));
        cut.setRelevantDocuments(judgements);

        cut.setTotalHits(FIFTEEN_SEARCH_HITS.length, A_VERSION);
        stream(FIFTEEN_SEARCH_HITS)
                .map(this::searchHit)
                .forEach(hit -> cut.collect(hit, counter.incrementAndGet(), A_VERSION));

        assertEquals(
                0.32,
                cut.valueFactory(A_VERSION).value().doubleValue(),
                0);
    }

    /**
     * Scenario: 10 judgments, 15 search results, 5 relevant results in top positions.
     */
    @Test
    public void _10_judgments_15_search_only_results_10th_relevant_result() {
        final ObjectNode judgements = mapper.createObjectNode();
        stream(FIFTEEN_SEARCH_HITS).limit(10).forEach(docid -> judgements.set(docid, createJudgmentNode(3)));
        cut.setRelevantDocuments(judgements);

        cut.setTotalHits(FIFTEEN_SEARCH_HITS.length, A_VERSION);
        stream(ANOTHER_FIVE_SEARCH_HITS)
                .map(this::searchHit)
                .forEach(hit -> cut.collect(hit, counter.incrementAndGet(), A_VERSION));

        stream(ANOTHER_FOUR_SEARCH_HITS)
                .map(this::searchHit)
                .forEach(hit -> cut.collect(hit, counter.incrementAndGet(), A_VERSION));

        cut.collect(searchHit(FIFTEEN_SEARCH_HITS[9]), counter.incrementAndGet(), A_VERSION);

        assertEquals(
                0.05,
                cut.valueFactory(A_VERSION).value().doubleValue(),
                0);
    }
}