package com.densitymst.core;

import com.densitymst.model.Graph;
import com.densitymst.model.MSTResult;

/**
 * Common interface for all MST algorithm implementations.
 */
public interface MSTAlgorithm {

    /**
     * Computes the MST of the given graph.
     *
     * @param graph the input graph (must be connected)
     * @return the MST result containing selected edges, weight, and timing
     */
    MSTResult compute(Graph graph);
}
