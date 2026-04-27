package com.densitymst.core;

import com.densitymst.model.Graph;
import com.densitymst.model.Vertex;
import com.densitymst.model.Edge;

import java.util.*;

/**
 * Validates graph properties before MST execution.
 * Uses BFS to determine connectivity.
 */
public class GraphValidator {

    /**
     * Checks if the given graph is connected.
     * A graph is connected if every vertex is reachable from any other vertex.
     *
     * @param graph the graph to validate
     * @return true if the graph is connected, false otherwise
     */
    public boolean isConnected(Graph graph) {
        List<Vertex> vertices = graph.getVertices();
        if (vertices.isEmpty()) return false;
        if (vertices.size() == 1) return true;

        // Build adjacency list
        Map<String, List<String>> adjacency = new HashMap<>();
        for (Vertex v : vertices) {
            adjacency.put(v.getId(), new ArrayList<>());
        }
        for (Edge e : graph.getEdges()) {
            adjacency.get(e.getSource().getId()).add(e.getDestination().getId());
            adjacency.get(e.getDestination().getId()).add(e.getSource().getId());
        }

        // BFS from first vertex
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        String startId = vertices.get(0).getId();
        queue.add(startId);
        visited.add(startId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String neighbor : adjacency.getOrDefault(current, Collections.emptyList())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return visited.size() == vertices.size();
    }
}
