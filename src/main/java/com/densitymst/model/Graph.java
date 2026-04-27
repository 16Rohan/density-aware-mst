package com.densitymst.model;

import java.util.ArrayList;
import java.util.List;

public class Graph {
    private final List<Vertex> vertices;
    private final List<Edge> edges;

    public Graph() {
        this.vertices = new ArrayList<>();
        this.edges = new ArrayList<>();
    }

    public void addVertex(Vertex v) {
        if (!vertices.contains(v)) {
            vertices.add(v);
        }
    }

    public void addEdge(Edge e) {
        edges.add(e);
    }

    public void removeVertex(Vertex v) {
        vertices.remove(v);
        edges.removeIf(e -> e.getSource().equals(v) || e.getDestination().equals(v));
    }

    public void removeEdge(Edge e) {
        edges.remove(e);
    }

    public List<Vertex> getVertices() { return vertices; }
    public List<Edge> getEdges() { return edges; }

    public void clearMSTFlags() {
        for (Edge e : edges) {
            e.setMSTEdge(false);
        }
    }
}
