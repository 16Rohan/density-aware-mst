package com.densitymst.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a single execution history entry.
 * Wraps a graph snapshot along with the MST results and metadata.
 */
public class HistoryItem {
    private String id;
    private String timestamp;
    private String inputMethod;
    private int vertexCount;
    private int edgeCount;
    private double density;
    private String selectedAlgorithm;
    private double mstWeight;
    private long runtimeMs;

    // Serialized graph data (adjacency matrix representation for persistence)
    private List<String> vertexIds;
    private List<double[]> vertexPositions;
    private List<int[]> edgeIndices; // [srcIndex, dstIndex]
    private List<Double> edgeWeights;

    public HistoryItem() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getInputMethod() { return inputMethod; }
    public void setInputMethod(String inputMethod) { this.inputMethod = inputMethod; }
    public int getVertexCount() { return vertexCount; }
    public void setVertexCount(int vertexCount) { this.vertexCount = vertexCount; }
    public int getEdgeCount() { return edgeCount; }
    public void setEdgeCount(int edgeCount) { this.edgeCount = edgeCount; }
    public double getDensity() { return density; }
    public void setDensity(double density) { this.density = density; }
    public String getSelectedAlgorithm() { return selectedAlgorithm; }
    public void setSelectedAlgorithm(String selectedAlgorithm) { this.selectedAlgorithm = selectedAlgorithm; }
    public double getMstWeight() { return mstWeight; }
    public void setMstWeight(double mstWeight) { this.mstWeight = mstWeight; }
    public long getRuntimeMs() { return runtimeMs; }
    public void setRuntimeMs(long runtimeMs) { this.runtimeMs = runtimeMs; }
    public List<String> getVertexIds() { return vertexIds; }
    public void setVertexIds(List<String> vertexIds) { this.vertexIds = vertexIds; }
    public List<double[]> getVertexPositions() { return vertexPositions; }
    public void setVertexPositions(List<double[]> vertexPositions) { this.vertexPositions = vertexPositions; }
    public List<int[]> getEdgeIndices() { return edgeIndices; }
    public void setEdgeIndices(List<int[]> edgeIndices) { this.edgeIndices = edgeIndices; }
    public List<Double> getEdgeWeights() { return edgeWeights; }
    public void setEdgeWeights(List<Double> edgeWeights) { this.edgeWeights = edgeWeights; }

    /**
     * Reconstructs a Graph object from the serialized data in this history item.
     */
    public Graph toGraph() {
        Graph graph = new Graph();
        if (vertexIds == null) return graph;

        Vertex[] vArray = new Vertex[vertexIds.size()];
        for (int i = 0; i < vertexIds.size(); i++) {
            double x = (vertexPositions != null && i < vertexPositions.size()) ? vertexPositions.get(i)[0] : 0;
            double y = (vertexPositions != null && i < vertexPositions.size()) ? vertexPositions.get(i)[1] : 0;
            vArray[i] = new Vertex(vertexIds.get(i), x, y);
            graph.addVertex(vArray[i]);
        }

        if (edgeIndices != null && edgeWeights != null) {
            for (int i = 0; i < edgeIndices.size(); i++) {
                int src = edgeIndices.get(i)[0];
                int dst = edgeIndices.get(i)[1];
                double w = edgeWeights.get(i);
                graph.addEdge(new Edge(vArray[src], vArray[dst], w));
            }
        }

        return graph;
    }

    /**
     * Creates a HistoryItem from a Graph and its MST result.
     */
    public static HistoryItem fromGraph(Graph graph, MSTResult result, String inputMethod, double density) {
        HistoryItem item = new HistoryItem();
        item.setId(java.util.UUID.randomUUID().toString());
        item.setTimestamp(LocalDateTime.now().toString());
        item.setInputMethod(inputMethod);
        item.setVertexCount(graph.getVertices().size());
        item.setEdgeCount(graph.getEdges().size());
        item.setDensity(density);
        item.setSelectedAlgorithm(result.getAlgorithmName());
        item.setMstWeight(result.getTotalWeight());
        item.setRuntimeMs(result.getRuntimeMs());

        // Serialize vertices
        List<Vertex> vertices = graph.getVertices();
        java.util.Map<String, Integer> idMap = new java.util.HashMap<>();
        item.setVertexIds(new java.util.ArrayList<>());
        item.setVertexPositions(new java.util.ArrayList<>());
        for (int i = 0; i < vertices.size(); i++) {
            Vertex v = vertices.get(i);
            idMap.put(v.getId(), i);
            item.getVertexIds().add(v.getId());
            item.getVertexPositions().add(new double[]{v.getX(), v.getY()});
        }

        // Serialize edges
        item.setEdgeIndices(new java.util.ArrayList<>());
        item.setEdgeWeights(new java.util.ArrayList<>());
        for (Edge e : graph.getEdges()) {
            int srcIdx = idMap.get(e.getSource().getId());
            int dstIdx = idMap.get(e.getDestination().getId());
            item.getEdgeIndices().add(new int[]{srcIdx, dstIdx});
            item.getEdgeWeights().add(e.getWeight());
        }

        return item;
    }
}
