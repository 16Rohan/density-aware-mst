package com.densitymst.model;

public class Edge implements Comparable<Edge> {
    private final Vertex source;
    private final Vertex destination;
    private final double weight;
    private boolean isMSTEdge;

    public Edge(Vertex source, Vertex destination, double weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
        this.isMSTEdge = false;
    }

    public Vertex getSource() { return source; }
    public Vertex getDestination() { return destination; }
    public double getWeight() { return weight; }
    public boolean isMSTEdge() { return isMSTEdge; }
    public void setMSTEdge(boolean MSTEdge) { isMSTEdge = MSTEdge; }

    @Override
    public int compareTo(Edge other) {
        return Double.compare(this.weight, other.weight);
    }
}
