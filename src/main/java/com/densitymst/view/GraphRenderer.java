package com.densitymst.view;

import com.densitymst.model.Edge;
import com.densitymst.model.Graph;
import com.densitymst.model.MSTResult;
import com.densitymst.model.Vertex;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.*;

/**
 * Renders the graph on a JavaFX Canvas with a live force-directed simulation.
 * Vertices are interactive and draggable. MST edges animate in sequentially.
 */
public class GraphRenderer {

    // Colors from colorscheme.css (light theme)
    private static final Color COLOR_CANVAS_BG = Color.web("#ffffff");
    private static final Color COLOR_VERTEX_DEFAULT = Color.web("#2f8d46");
    private static final Color COLOR_VERTEX_MST = Color.web("#1f6f3a");
    private static final Color COLOR_EDGE_DEFAULT = Color.web("#c0c4c8");
    private static final Color COLOR_EDGE_MST = Color.web("#2f8d46");
    private static final Color COLOR_EDGE_EXCLUDED = Color.web("#b0b3b8", 0.6);
    private static final Color COLOR_WEIGHT_LABEL = Color.web("#495057");
    private static final Color COLOR_WEIGHT_MST = Color.web("#ffffff");
    private static final Color COLOR_VERTEX_LABEL = Color.web("#ffffff");
    private static final Color COLOR_GRID_DOT = Color.web("#edeef0");

    // Visual sizes (1.5x scaled)
    private static final double VERTEX_RADIUS = 21;
    private static final double EDGE_WIDTH_DEFAULT = 2.25;
    private static final double EDGE_WIDTH_MST = 4.5;
    private static final double PADDING = 25;
    private static final double WEIGHT_FONT_SIZE = 15;
    private static final double LABEL_FONT_SIZE = 16;

    // Live force simulation parameters
    private static final double REPULSION = 6500;
    private static final double ATTRACTION = 0.008;
    private static final double GRAVITY = 0.002;
    private static final double FRICTION = 0.85;
    private static final double MIN_DIST = 50;
    private static final double MAX_VELOCITY = 8.0;
    private static final double SETTLE_THRESHOLD = 0.05;
    // Edge-weight repulsion: heavier edges push endpoints further apart
    private static final double BASE_EDGE_REPULSION = 1200;
    private static final double WEIGHT_REPULSION_SCALE = 400;

    private final Canvas canvas;
    private Graph currentGraph;
    private MSTResult currentResult;

    // Live simulation state
    private AnimationTimer simTimer;
    private Map<String, double[]> velocities = new HashMap<>();
    private boolean simulationActive = false;
    private int settledFrames = 0;

    // Drag state
    private Vertex draggedVertex = null;

    // MST animation state
    private Timeline mstTimeline;
    private List<Edge> animatedMstEdges = new ArrayList<>();

    public GraphRenderer(Canvas canvas) {
        this.canvas = canvas;
        initSimulationTimer();
    }

    private void initSimulationTimer() {
        simTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (currentGraph == null || currentGraph.getVertices().isEmpty())
                    return;
                applyForces();
                render();
            }
        };
    }

    // ─── Public API ───

    /**
     * Sets a new graph and starts the live force simulation.
     */
    public void drawGraph(Graph graph) {
        stopMSTAnimation();
        this.currentGraph = graph;
        this.currentResult = null;
        this.animatedMstEdges.clear();
        if (graph != null && !graph.getVertices().isEmpty()) {
            initializePositions(graph);
            startSimulation();
        } else {
            stopSimulation();
            render();
        }
    }

    /**
     * Highlights MST edges with step-by-step animation.
     */
    public void drawMST(Graph graph, MSTResult result) {
        stopMSTAnimation();
        this.currentGraph = graph;
        this.currentResult = result;
        this.animatedMstEdges.clear();
        if (result != null && !result.getMstEdges().isEmpty()) {
            animateMST(result);
        }
        // Simulation keeps running so nodes stay interactive
        if (!simulationActive && graph != null && !graph.getVertices().isEmpty()) {
            startSimulation();
        }
    }

    /**
     * Draws MST immediately without animation (for resize redraws).
     */
    public void drawMSTImmediate(Graph graph, MSTResult result) {
        stopMSTAnimation();
        this.currentGraph = graph;
        this.currentResult = result;
        if (result != null) {
            this.animatedMstEdges = new ArrayList<>(result.getMstEdges());
        }
        render();
    }

    /**
     * Clears everything and stops the simulation.
     */
    public void clear() {
        stopSimulation();
        stopMSTAnimation();
        this.currentGraph = null;
        this.currentResult = null;
        this.animatedMstEdges.clear();
        this.velocities.clear();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(COLOR_CANVAS_BG);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    // ─── Mouse Interaction ───

    public void onMousePressed(double x, double y) {
        if (currentGraph == null)
            return;
        draggedVertex = findVertexAt(x, y);
    }

    public void onMouseDragged(double x, double y) {
        if (draggedVertex != null) {
            draggedVertex.setX(x);
            draggedVertex.setY(y);
            // Zero out velocity so it doesn't fly off on release
            double[] vel = velocities.get(draggedVertex.getId());
            if (vel != null) {
                vel[0] = 0;
                vel[1] = 0;
            }
            settledFrames = 0;
            render();
        }
    }

    public void onMouseReleased() {
        draggedVertex = null;
    }

    public Vertex findVertexAt(double x, double y) {
        if (currentGraph == null)
            return null;
        for (Vertex v : currentGraph.getVertices()) {
            double dx = v.getX() - x, dy = v.getY() - y;
            if (Math.sqrt(dx * dx + dy * dy) <= VERTEX_RADIUS + 5)
                return v;
        }
        return null;
    }

    // ─── Force Simulation ───

    private void startSimulation() {
        settledFrames = 0;
        simulationActive = true;
        simTimer.start();
    }

    private void stopSimulation() {
        simulationActive = false;
        simTimer.stop();
    }

    private void initializePositions(Graph graph) {
        List<Vertex> verts = graph.getVertices();
        int n = verts.size();
        double cw = Math.max(canvas.getWidth(), 200);
        double ch = Math.max(canvas.getHeight(), 200);
        double cx = cw / 2.0;
        double cy = ch / 2.0;
        double radius = Math.min(cx, cy) * 0.45;

        velocities.clear();
        for (int i = 0; i < n; i++) {
            Vertex v = verts.get(i);
            // Place uninitialized vertices in a circle, keep existing ones
            if (v.getX() == 0 && v.getY() == 0) {
                double angle = 2 * Math.PI * i / n + (Math.random() * 0.3 - 0.15);
                v.setX(cx + radius * Math.cos(angle));
                v.setY(cy + radius * Math.sin(angle));
            }
            velocities.put(v.getId(), new double[] { 0, 0 });
        }
    }

    private void applyForces() {
        List<Vertex> verts = currentGraph.getVertices();
        int n = verts.size();
        if (n < 2)
            return;

        double cw = Math.max(canvas.getWidth(), 200);
        double ch = Math.max(canvas.getHeight(), 200);
        double cx = cw / 2.0;
        double cy = ch / 2.0;

        double totalKE = 0;

        // Repulsion between all vertex pairs
        for (int i = 0; i < n; i++) {
            Vertex vi = verts.get(i);
            if (vi == draggedVertex)
                continue;
            for (int j = i + 1; j < n; j++) {
                Vertex vj = verts.get(j);
                double dx = vi.getX() - vj.getX();
                double dy = vi.getY() - vj.getY();
                double dist = Math.max(Math.sqrt(dx * dx + dy * dy), MIN_DIST);
                double force = REPULSION / (dist * dist);
                double fx = force * dx / dist;
                double fy = force * dy / dist;

                if (vi != draggedVertex) {
                    velocities.get(vi.getId())[0] += fx;
                    velocities.get(vi.getId())[1] += fy;
                }
                if (vj != draggedVertex) {
                    velocities.get(vj.getId())[0] -= fx;
                    velocities.get(vj.getId())[1] -= fy;
                }
            }
        }

        // Attraction along edges + weight-proportional repulsion
        for (Edge edge : currentGraph.getEdges()) {
            Vertex vs = edge.getSource();
            Vertex vd = edge.getDestination();
            double dx = vd.getX() - vs.getX();
            double dy = vd.getY() - vs.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);

            // Attraction (pulls connected nodes together)
            double attractForce = ATTRACTION * dist;
            double afx = attractForce * dx / Math.max(dist, 1);
            double afy = attractForce * dy / Math.max(dist, 1);

            // Weight-proportional repulsion (heavier edge = more spacing)
            double repulForce = (BASE_EDGE_REPULSION + WEIGHT_REPULSION_SCALE * edge.getWeight())
                    / (Math.max(dist, MIN_DIST) * Math.max(dist, MIN_DIST));
            // Repulsion pushes apart: opposite direction of dx/dy from source's perspective
            double rfx = repulForce * (-dx) / Math.max(dist, 1);
            double rfy = repulForce * (-dy) / Math.max(dist, 1);

            // Net force on source: attraction toward dest + repulsion away from dest
            double netFxS = afx + rfx;
            double netFyS = afy + rfy;

            if (vs != draggedVertex) {
                velocities.get(vs.getId())[0] += netFxS;
                velocities.get(vs.getId())[1] += netFyS;
            }
            if (vd != draggedVertex) {
                velocities.get(vd.getId())[0] -= netFxS;
                velocities.get(vd.getId())[1] -= netFyS;
            }
        }

        // Gravity toward center
        for (Vertex v : verts) {
            if (v == draggedVertex)
                continue;
            double[] vel = velocities.get(v.getId());
            if (vel == null) {
                vel = new double[] { 0, 0 };
                velocities.put(v.getId(), vel);
            }
            vel[0] += (cx - v.getX()) * GRAVITY;
            vel[1] += (cy - v.getY()) * GRAVITY;
        }

        // Apply velocities with friction and clamping
        for (Vertex v : verts) {
            if (v == draggedVertex)
                continue;
            double[] vel = velocities.get(v.getId());
            if (vel == null)
                continue;

            vel[0] *= FRICTION;
            vel[1] *= FRICTION;
            vel[0] = Math.max(-MAX_VELOCITY, Math.min(MAX_VELOCITY, vel[0]));
            vel[1] = Math.max(-MAX_VELOCITY, Math.min(MAX_VELOCITY, vel[1]));

            v.setX(v.getX() + vel[0]);
            v.setY(v.getY() + vel[1]);

            // Keep inside canvas bounds
            v.setX(Math.max(PADDING, Math.min(cw - PADDING, v.getX())));
            v.setY(Math.max(PADDING, Math.min(ch - PADDING, v.getY())));

            totalKE += vel[0] * vel[0] + vel[1] * vel[1];
        }

        // Auto-throttle: if settled, slow down the timer
        if (totalKE < SETTLE_THRESHOLD) {
            settledFrames++;
            if (settledFrames > 120) {
                // Settled — keep timer running but do nothing until disturbed
                // (will re-activate on drag or vertex add)
            }
        } else {
            settledFrames = 0;
        }
    }

    // ─── MST Animation ───

    private void stopMSTAnimation() {
        if (mstTimeline != null) {
            mstTimeline.stop();
            mstTimeline = null;
        }
    }

    private void animateMST(MSTResult result) {
        List<Edge> edges = new ArrayList<>(result.getMstEdges());
        animatedMstEdges.clear();
        final int[] index = { 0 };

        mstTimeline = new Timeline();
        mstTimeline.setCycleCount(edges.size());

        KeyFrame kf = new KeyFrame(Duration.millis(350), event -> {
            if (index[0] < edges.size()) {
                animatedMstEdges.add(edges.get(index[0]));
                index[0]++;
                render();
            }
        });
        mstTimeline.getKeyFrames().add(kf);
        mstTimeline.play();
    }

    // ─── Rendering ───

    public void render() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0)
            return;
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Background
        gc.setFill(COLOR_CANVAS_BG);
        gc.fillRect(0, 0, w, h);

        // Subtle dot grid
        gc.setFill(COLOR_GRID_DOT);
        double gridSpacing = 30;
        for (double gx = gridSpacing; gx < w; gx += gridSpacing) {
            for (double gy = gridSpacing; gy < h; gy += gridSpacing) {
                gc.fillOval(gx - 1, gy - 1, 2, 2);
            }
        }

        if (currentGraph == null || currentGraph.getVertices().isEmpty())
            return;

        // Build MST edge sets
        Set<Edge> visibleMstEdges = new HashSet<>(animatedMstEdges);
        Set<Edge> allMstEdges = new HashSet<>();
        Set<Edge> excludedEdges = new HashSet<>();
        if (currentResult != null) {
            allMstEdges.addAll(currentResult.getMstEdges());
            excludedEdges.addAll(currentResult.getExcludedEdges());
        }

        // Draw edges: non-MST first
        for (Edge edge : currentGraph.getEdges()) {
            if (allMstEdges.contains(edge))
                continue;
            drawEdge(gc, edge, excludedEdges.contains(edge), false);
        }
        // Draw MST edges not yet animated as excluded (faded)
        for (Edge edge : allMstEdges) {
            if (!visibleMstEdges.contains(edge)) {
                drawEdge(gc, edge, true, false);
            }
        }
        // Draw visible MST edges highlighted
        for (Edge edge : animatedMstEdges) {
            drawEdge(gc, edge, false, true);
        }

        // Draw vertices
        Set<String> mstVertexIds = new HashSet<>();
        for (Edge e : visibleMstEdges) {
            mstVertexIds.add(e.getSource().getId());
            mstVertexIds.add(e.getDestination().getId());
        }
        for (Vertex vertex : currentGraph.getVertices()) {
            drawVertex(gc, vertex, mstVertexIds.contains(vertex.getId()));
        }
    }

    private void drawEdge(GraphicsContext gc, Edge edge, boolean isExcluded, boolean isMST) {
        double x1 = edge.getSource().getX();
        double y1 = edge.getSource().getY();
        double x2 = edge.getDestination().getX();
        double y2 = edge.getDestination().getY();

        if (isMST) {
            // Glow behind MST edge
            gc.setStroke(Color.web("#e8f5e9"));
            gc.setLineWidth(EDGE_WIDTH_MST + 8);
            gc.setGlobalAlpha(0.6);
            gc.strokeLine(x1, y1, x2, y2);

            gc.setStroke(COLOR_EDGE_MST);
            gc.setLineWidth(EDGE_WIDTH_MST);
        } else if (isExcluded) {
            gc.setStroke(COLOR_EDGE_EXCLUDED);
            gc.setLineWidth(EDGE_WIDTH_DEFAULT);
        } else {
            gc.setStroke(COLOR_EDGE_DEFAULT);
            gc.setLineWidth(EDGE_WIDTH_DEFAULT);
        }
        gc.setGlobalAlpha(1.0);
        gc.strokeLine(x1, y1, x2, y2);

        // Weight label at midpoint
        double mx = (x1 + x2) / 2;
        double my = (y1 + y2) / 2;
        String weightText = String.valueOf((int) edge.getWeight());
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, WEIGHT_FONT_SIZE));
        gc.setTextAlign(TextAlignment.CENTER);

        if (isMST) {
            gc.setFill(Color.web("#2f8d46", 0.9));
            gc.fillRoundRect(mx - 16, my - 12, 32, 18, 8, 8);
            gc.setFill(COLOR_WEIGHT_MST);
        } else if (!isExcluded) {
            gc.setFill(Color.web("#f1f3f5", 0.95));
            gc.fillRoundRect(mx - 14, my - 12, 28, 18, 8, 8);
            gc.setFill(COLOR_WEIGHT_LABEL);
        }

        if (!isExcluded) {
            gc.fillText(weightText, mx, my + 1);
        }
    }

    private void drawVertex(GraphicsContext gc, Vertex vertex, boolean isMST) {
        double cx = vertex.getX();
        double cy = vertex.getY();
        boolean isDragged = (vertex == draggedVertex);

        // Outer glow
        if (isMST || isDragged) {
            gc.setGlobalAlpha(isDragged ? 0.5 : 0.35);
            gc.setFill(Color.web("#e8f5e9"));
            gc.fillOval(cx - VERTEX_RADIUS * 2.2, cy - VERTEX_RADIUS * 2.2,
                    VERTEX_RADIUS * 4.4, VERTEX_RADIUS * 4.4);
        } else {
            gc.setGlobalAlpha(0.2);
            gc.setFill(Color.web("#e9ecef"));
            gc.fillOval(cx - VERTEX_RADIUS * 1.6, cy - VERTEX_RADIUS * 1.6,
                    VERTEX_RADIUS * 3.2, VERTEX_RADIUS * 3.2);
        }
        gc.setGlobalAlpha(1.0);

        // Drop shadow
        gc.setFill(Color.web("#000000", 0.08));
        gc.fillOval(cx - VERTEX_RADIUS + 2, cy - VERTEX_RADIUS + 2,
                VERTEX_RADIUS * 2, VERTEX_RADIUS * 2);

        // Vertex circle
        gc.setFill(isMST ? COLOR_VERTEX_MST : COLOR_VERTEX_DEFAULT);
        gc.fillOval(cx - VERTEX_RADIUS, cy - VERTEX_RADIUS,
                VERTEX_RADIUS * 2, VERTEX_RADIUS * 2);

        // Inner highlight
        gc.setGlobalAlpha(0.25);
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - VERTEX_RADIUS * 0.5, cy - VERTEX_RADIUS * 0.65,
                VERTEX_RADIUS * 0.9, VERTEX_RADIUS * 0.55);
        gc.setGlobalAlpha(1.0);

        // Vertex label
        gc.setFill(COLOR_VERTEX_LABEL);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, LABEL_FONT_SIZE));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(vertex.getId(), cx, cy + 5);
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public Graph getCurrentGraph() {
        return currentGraph;
    }

    public MSTResult getCurrentResult() {
        return currentResult;
    }
}
