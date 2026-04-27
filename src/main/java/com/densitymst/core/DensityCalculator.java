package com.densitymst.core;

import com.densitymst.model.Graph;

/**
 * Calculates graph density using the formula: 2E / (V × (V - 1)).
 */
public class DensityCalculator {

    /**
     * Computes the density of the given graph.
     *
     * @param graph the graph whose density to compute
     * @return density value between 0.0 and 1.0
     */
    public double calculate(Graph graph) {
        int v = graph.getVertices().size();
        int e = graph.getEdges().size();
        if (v <= 1) return 0.0;
        return (2.0 * e) / (v * (v - 1));
    }
}
