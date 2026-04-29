package com.densitymst.core;

import com.densitymst.model.Edge;
import com.densitymst.model.Graph;
import com.densitymst.model.MSTResult;
import com.densitymst.model.Vertex;

import java.util.*;

/**
 * Kruskal's MST algorithm using Disjoint Set Union (DSU) with path compression
 * and union by rank. Best for sparse graphs.
 */
public class KruskalAlgorithm implements MSTAlgorithm {

    private Map<String, String> parent;
    private Map<String, Integer> rank;

    // Step counters
    private long comparisons;
    private long unionFindOps;

    @Override
    public MSTResult compute(Graph graph) {
        long startTime = System.nanoTime();

        comparisons = 0;
        unionFindOps = 0;

        List<Edge> sortedEdges = new ArrayList<>(graph.getEdges());

        // Sort with a counting comparator to track comparisons
        sortedEdges.sort((a, b) -> {
            comparisons++;
            return Double.compare(a.getWeight(), b.getWeight());
        });

        // We already track actual comparisons, so no need to double-count theoretical sort ops.
        long sortOps = 0;

        // Initialize DSU
        parent = new HashMap<>();
        rank = new HashMap<>();
        for (Vertex v : graph.getVertices()) {
            parent.put(v.getId(), v.getId());
            rank.put(v.getId(), 0);
        }

        List<Edge> mstEdges = new ArrayList<>();
        List<Edge> excludedEdges = new ArrayList<>();
        double totalWeight = 0;

        for (Edge edge : sortedEdges) {
            String rootSrc = find(edge.getSource().getId());
            String rootDst = find(edge.getDestination().getId());

            comparisons++; // comparing roots for equality
            if (!rootSrc.equals(rootDst)) {
                mstEdges.add(edge);
                totalWeight += edge.getWeight();
                union(rootSrc, rootDst);
            } else {
                excludedEdges.add(edge);
            }
        }

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        return new MSTResult("Kruskal", elapsed, mstEdges, excludedEdges, totalWeight,
                comparisons, sortOps, unionFindOps, 0);
    }

    private String find(String x) {
        unionFindOps++;
        if (!parent.get(x).equals(x)) {
            parent.put(x, find(parent.get(x))); // Path compression
        }
        return parent.get(x);
    }

    private void union(String a, String b) {
        unionFindOps++;
        int rankA = rank.get(a);
        int rankB = rank.get(b);
        if (rankA < rankB) {
            parent.put(a, b);
        } else if (rankA > rankB) {
            parent.put(b, a);
        } else {
            parent.put(b, a);
            rank.put(a, rankA + 1);
        }
    }
}
