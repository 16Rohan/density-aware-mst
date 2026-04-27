package com.densitymst.service;

import com.densitymst.model.Edge;
import com.densitymst.model.Graph;
import com.densitymst.model.Vertex;

import java.util.*;

/**
 * Generates random connected graphs and provides sample preset graphs for testing.
 */
public class GraphGenerator {

    private final Random random = new Random();

    /**
     * Generates a random connected graph with the specified number of vertices and edges.
     * First creates a spanning tree to guarantee connectivity, then adds remaining random edges.
     *
     * @param vertexCount number of vertices (must be >= 2)
     * @param edgeCount   number of edges (must be >= vertexCount - 1 and <= V*(V-1)/2)
     * @return a connected weighted graph
     * @throws IllegalArgumentException if parameters are invalid
     */
    public Graph generateRandom(int vertexCount, int edgeCount) {
        int maxEdges = vertexCount * (vertexCount - 1) / 2;
        if (vertexCount < 2) throw new IllegalArgumentException("Need at least 2 vertices.");
        if (edgeCount < vertexCount - 1) throw new IllegalArgumentException("Need at least " + (vertexCount - 1) + " edges for connectivity.");
        if (edgeCount > maxEdges) throw new IllegalArgumentException("Max edges for " + vertexCount + " vertices is " + maxEdges + ".");

        Graph graph = new Graph();

        // Create vertices in a circular layout
        Vertex[] vertices = new Vertex[vertexCount];
        double centerX = 350, centerY = 300, radius = 200;
        for (int i = 0; i < vertexCount; i++) {
            double angle = 2 * Math.PI * i / vertexCount;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            vertices[i] = new Vertex("V" + i, x, y);
            graph.addVertex(vertices[i]);
        }

        // Create spanning tree (random permutation) to ensure connectivity
        Set<String> edgeSet = new HashSet<>();
        List<Integer> perm = new ArrayList<>();
        for (int i = 0; i < vertexCount; i++) perm.add(i);
        Collections.shuffle(perm, random);

        for (int i = 1; i < vertexCount; i++) {
            int u = perm.get(i - 1);
            int v = perm.get(i);
            int a = Math.min(u, v), b = Math.max(u, v);
            double weight = 1 + random.nextInt(20);
            graph.addEdge(new Edge(vertices[a], vertices[b], weight));
            edgeSet.add(a + "-" + b);
        }

        // Add remaining random edges
        int remaining = edgeCount - (vertexCount - 1);
        while (remaining > 0) {
            int u = random.nextInt(vertexCount);
            int v = random.nextInt(vertexCount);
            if (u == v) continue;
            int a = Math.min(u, v), b = Math.max(u, v);
            String key = a + "-" + b;
            if (edgeSet.contains(key)) continue;
            double weight = 1 + random.nextInt(20);
            graph.addEdge(new Edge(vertices[a], vertices[b], weight));
            edgeSet.add(key);
            remaining--;
        }

        return graph;
    }

    /**
     * Returns a sample dense graph #1 (5 vertices, 9 edges).
     */
    public Graph sampleDense1() {
        Graph g = new Graph();
        Vertex[] v = createSampleVertices(g, 5);
        addEdge(g, v, 0, 1, 2);
        addEdge(g, v, 0, 2, 3);
        addEdge(g, v, 0, 3, 6);
        addEdge(g, v, 1, 2, 5);
        addEdge(g, v, 1, 3, 8);
        addEdge(g, v, 1, 4, 3);
        addEdge(g, v, 2, 3, 7);
        addEdge(g, v, 2, 4, 9);
        addEdge(g, v, 3, 4, 4);
        return g;
    }

    /**
     * Returns a sample dense graph #2 (6 vertices, 12 edges).
     */
    public Graph sampleDense2() {
        Graph g = new Graph();
        Vertex[] v = createSampleVertices(g, 6);
        addEdge(g, v, 0, 1, 4);
        addEdge(g, v, 0, 2, 1);
        addEdge(g, v, 0, 3, 5);
        addEdge(g, v, 1, 2, 2);
        addEdge(g, v, 1, 4, 7);
        addEdge(g, v, 2, 3, 8);
        addEdge(g, v, 2, 4, 3);
        addEdge(g, v, 2, 5, 6);
        addEdge(g, v, 3, 5, 9);
        addEdge(g, v, 4, 5, 1);
        addEdge(g, v, 0, 4, 10);
        addEdge(g, v, 1, 5, 11);
        return g;
    }

    /**
     * Returns a sample sparse graph #1 (6 vertices, 6 edges).
     */
    public Graph sampleSparse1() {
        Graph g = new Graph();
        Vertex[] v = createSampleVertices(g, 6);
        addEdge(g, v, 0, 1, 3);
        addEdge(g, v, 1, 2, 5);
        addEdge(g, v, 2, 3, 2);
        addEdge(g, v, 3, 4, 7);
        addEdge(g, v, 4, 5, 4);
        addEdge(g, v, 5, 0, 6);
        return g;
    }

    /**
     * Returns a sample sparse graph #2 (7 vertices, 8 edges).
     */
    public Graph sampleSparse2() {
        Graph g = new Graph();
        Vertex[] v = createSampleVertices(g, 7);
        addEdge(g, v, 0, 1, 2);
        addEdge(g, v, 1, 2, 4);
        addEdge(g, v, 2, 3, 1);
        addEdge(g, v, 3, 4, 6);
        addEdge(g, v, 4, 5, 3);
        addEdge(g, v, 5, 6, 5);
        addEdge(g, v, 6, 0, 8);
        addEdge(g, v, 0, 3, 7);
        return g;
    }

    private Vertex[] createSampleVertices(Graph g, int count) {
        Vertex[] vertices = new Vertex[count];
        double centerX = 350, centerY = 300, radius = 200;
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            vertices[i] = new Vertex("V" + i, x, y);
            g.addVertex(vertices[i]);
        }
        return vertices;
    }

    private void addEdge(Graph g, Vertex[] v, int src, int dst, double weight) {
        g.addEdge(new Edge(v[src], v[dst], weight));
    }
}
