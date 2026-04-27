package com.densitymst.model;

import java.util.List;

public class MSTResult {
    private final String algorithmName;
    private final long runtimeMs;
    private final List<Edge> mstEdges;
    private final List<Edge> excludedEdges;
    private final double totalWeight;

    // Quantifiable step-level metrics
    private final long comparisons;       // edge weight comparisons
    private final long heapOperations;    // PQ inserts + extractions (Prim) / sort operations (Kruskal)
    private final long unionFindOps;      // union + find calls (Kruskal only)
    private final long keyUpdates;        // key relaxation updates (Prim Basic only)

    public MSTResult(String algorithmName, long runtimeMs, List<Edge> mstEdges,
                     List<Edge> excludedEdges, double totalWeight,
                     long comparisons, long heapOperations,
                     long unionFindOps, long keyUpdates) {
        this.algorithmName = algorithmName;
        this.runtimeMs = runtimeMs;
        this.mstEdges = mstEdges;
        this.excludedEdges = excludedEdges;
        this.totalWeight = totalWeight;
        this.comparisons = comparisons;
        this.heapOperations = heapOperations;
        this.unionFindOps = unionFindOps;
        this.keyUpdates = keyUpdates;
    }

    public String getAlgorithmName() { return algorithmName; }
    public long getRuntimeMs() { return runtimeMs; }
    public List<Edge> getMstEdges() { return mstEdges; }
    public List<Edge> getExcludedEdges() { return excludedEdges; }
    public double getTotalWeight() { return totalWeight; }
    public long getComparisons() { return comparisons; }
    public long getHeapOperations() { return heapOperations; }
    public long getUnionFindOps() { return unionFindOps; }
    public long getKeyUpdates() { return keyUpdates; }
}
