package com.densitymst.core;

import com.densitymst.model.Edge;
import com.densitymst.model.Graph;
import com.densitymst.model.MSTResult;
import com.densitymst.model.Vertex;

import java.util.*;

/**
 * Prim's MST algorithm using a min-heap (PriorityQueue).
 * Time complexity: O(E log V). Best for dense graphs.
 */
public class PrimMinHeapAlgorithm implements MSTAlgorithm {

    @Override
    public MSTResult compute(Graph graph) {
        long startTime = System.nanoTime();

        List<Vertex> vertices = graph.getVertices();
        if (vertices.isEmpty()) {
            return new MSTResult("Prim (Min-Heap)", 0, List.of(), List.of(), 0);
        }

        // Build adjacency map: vertexId -> list of edges
        Map<String, List<Edge>> adjacency = new HashMap<>();
        for (Vertex v : vertices) {
            adjacency.put(v.getId(), new ArrayList<>());
        }
        for (Edge e : graph.getEdges()) {
            adjacency.get(e.getSource().getId()).add(e);
            adjacency.get(e.getDestination().getId()).add(e);
        }

        Set<String> inMST = new HashSet<>();
        List<Edge> mstEdges = new ArrayList<>();
        double totalWeight = 0;

        // PriorityQueue of edges sorted by weight
        PriorityQueue<Edge> pq = new PriorityQueue<>();

        // Start from the first vertex
        String startId = vertices.get(0).getId();
        inMST.add(startId);
        pq.addAll(adjacency.get(startId));

        while (!pq.isEmpty() && inMST.size() < vertices.size()) {
            Edge cheapest = pq.poll();
            String srcId = cheapest.getSource().getId();
            String dstId = cheapest.getDestination().getId();

            // Determine which end is the new vertex
            String newVertex = null;
            if (inMST.contains(srcId) && !inMST.contains(dstId)) {
                newVertex = dstId;
            } else if (inMST.contains(dstId) && !inMST.contains(srcId)) {
                newVertex = srcId;
            }

            if (newVertex != null) {
                inMST.add(newVertex);
                mstEdges.add(cheapest);
                totalWeight += cheapest.getWeight();
                // Add all edges of the new vertex to the priority queue
                for (Edge adj : adjacency.get(newVertex)) {
                    String otherEnd = adj.getSource().getId().equals(newVertex)
                            ? adj.getDestination().getId()
                            : adj.getSource().getId();
                    if (!inMST.contains(otherEnd)) {
                        pq.add(adj);
                    }
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
        return new MSTResult("Prim (Min-Heap)", elapsed, mstEdges, excludedEdges, totalWeight);
    }
}
