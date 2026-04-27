package com.densitymst.model;

import java.util.List;

public class MSTResult {
    private final String algorithmName;
    private final long runtimeMs;
    private final List<Edge> mstEdges;
    private final List<Edge> excludedEdges;
    private final double totalWeight;

    public MSTResult(String algorithmName, long runtimeMs, List<Edge> mstEdges, List<Edge> excludedEdges, double totalWeight) {
        this.algorithmName = algorithmName;
        this.runtimeMs = runtimeMs;
        this.mstEdges = mstEdges;
        this.excludedEdges = excludedEdges;
        this.totalWeight = totalWeight;
    }

    public String getAlgorithmName() { return algorithmName; }
    public long getRuntimeMs() { return runtimeMs; }
    public List<Edge> getMstEdges() { return mstEdges; }
    public List<Edge> getExcludedEdges() { return excludedEdges; }
    public double getTotalWeight() { return totalWeight; }
}
