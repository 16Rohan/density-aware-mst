package com.densitymst.core;

import com.densitymst.model.Graph;

/**
 * Selects the optimal MST algorithm based on graph density.
 * Uses the threshold E ≈ V² / log V to decide between Kruskal and Prim.
 */
public class AlgorithmSelector {

    /**
     * Determines which MST algorithm is most suitable for the given graph.
     *
     * @param graph the input graph
     * @return "KRUSKAL" for sparse graphs, "PRIM" for dense graphs
     */
    public String select(Graph graph) {
        int v = graph.getVertices().size();
        int e = graph.getEdges().size();

        if (v <= 2) return "KRUSKAL";

        double threshold = (double)(v * v) / Math.log(v);
        return (e <= threshold) ? "KRUSKAL" : "PRIM";
    }

    /**
     * Returns a human-readable explanation for the algorithm choice.
     *
     * @param graph the input graph
     * @param density the pre-calculated density value
     * @return explanation string
     */
    public String explain(Graph graph, double density) {
        String algo = select(graph);
        if ("KRUSKAL".equals(algo)) {
            return String.format("Sparse graph detected (density=%.4f); Kruskal selected.", density);
        } else {
            return String.format("Dense graph detected (density=%.4f); Prim selected.", density);
        }
    }
}
