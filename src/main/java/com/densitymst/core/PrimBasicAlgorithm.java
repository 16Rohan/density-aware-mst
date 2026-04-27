package com.densitymst.core;

import com.densitymst.model.Edge;
import com.densitymst.model.Graph;
import com.densitymst.model.MSTResult;
import com.densitymst.model.Vertex;

import java.util.*;

/**
 * Prim's MST algorithm using basic O(V²) implementation.
 * Used as a benchmark baseline for comparison.
 */
public class PrimBasicAlgorithm implements MSTAlgorithm {

    @Override
    public MSTResult compute(Graph graph) {
        long startTime = System.nanoTime();

        List<Vertex> vertices = graph.getVertices();
        if (vertices.isEmpty()) {
            return new MSTResult("Prim (Basic)", 0, List.of(), List.of(), 0);
        }

        int n = vertices.size();
        Map<String, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            idToIndex.put(vertices.get(i).getId(), i);
        }

        // Build adjacency matrix
        double[][] adjMatrix = new double[n][n];
        Edge[][] edgeRef = new Edge[n][n];
        for (double[] row : adjMatrix) Arrays.fill(row, Double.MAX_VALUE);

        for (Edge e : graph.getEdges()) {
            int u = idToIndex.get(e.getSource().getId());
            int v = idToIndex.get(e.getDestination().getId());
            if (e.getWeight() < adjMatrix[u][v]) {
                adjMatrix[u][v] = e.getWeight();
                adjMatrix[v][u] = e.getWeight();
                edgeRef[u][v] = e;
                edgeRef[v][u] = e;
            }
        }

        boolean[] inMST = new boolean[n];
        double[] key = new double[n];
        int[] parent = new int[n];
        Arrays.fill(key, Double.MAX_VALUE);
        Arrays.fill(parent, -1);

        key[0] = 0;
        List<Edge> mstEdges = new ArrayList<>();
        double totalWeight = 0;

        for (int count = 0; count < n; count++) {
            // Pick minimum key vertex not yet in MST
            int u = -1;
            double minKey = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (!inMST[i] && key[i] < minKey) {
                    minKey = key[i];
                    u = i;
                }
            }

            if (u == -1) break;
            inMST[u] = true;

            if (parent[u] != -1) {
                Edge selectedEdge = edgeRef[parent[u]][u];
                if (selectedEdge != null) {
                    mstEdges.add(selectedEdge);
                    totalWeight += selectedEdge.getWeight();
                }
            }

            // Update keys of adjacent vertices
            for (int v = 0; v < n; v++) {
                if (!inMST[v] && adjMatrix[u][v] < key[v]) {
                    key[v] = adjMatrix[u][v];
                    parent[v] = u;
                }
            }
        }

        // Determine excluded edges
        Set<Edge> mstSet = new HashSet<>(mstEdges);
        List<Edge> excludedEdges = new ArrayList<>();
        for (Edge e : graph.getEdges()) {
            if (!mstSet.contains(e)) {
                excludedEdges.add(e);
            }
        }

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        return new MSTResult("Prim (Basic)", elapsed, mstEdges, excludedEdges, totalWeight);
    }
}
