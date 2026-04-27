package com.densitymst.controller;

import com.densitymst.core.*;
import com.densitymst.model.*;
import com.densitymst.service.*;
import com.densitymst.view.GraphRenderer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;

/**
 * Main controller orchestrating the entire application UI and logic.
 */
public class MainController {

    // Colors from colorscheme.css (light theme)
    private static final String BG_PRIMARY   = "#ffffff";
    private static final String BG_SECONDARY = "#f8f9fa";
    private static final String BG_TERTIARY  = "#f1f3f5";
    private static final String GFG_GREEN    = "#2f8d46";
    private static final String TEXT_PRIMARY  = "#212529";
    private static final String TEXT_SECONDARY= "#6c757d";
    private static final String TEXT_MUTED    = "#9aa0a6";
    private static final String SUCCESS      = "#198754";

    private final BorderPane root;
    private final GraphRenderer renderer;
    private final Canvas canvas;
    private final GraphValidator validator = new GraphValidator();
    private final DensityCalculator densityCalc = new DensityCalculator();
    private final AlgorithmSelector selector = new AlgorithmSelector();
    private final GraphGenerator generator = new GraphGenerator();
    private final HistoryManager historyManager = new HistoryManager();

    private Graph currentGraph = new Graph();
    private String currentInputMethod = "Visual Editor";

    // UI references
    private Label statusLabel;
    private Label densityLabel;
    private Label algoLabel;
    private Label vertexCountLabel;
    private Label edgeCountLabel;
    private TextArea matrixInput;
    private TextField vertexField;
    private TextField edgeField;
    private VBox historyList;
    private VBox benchmarkBox;
    private boolean isAddingEdge = false;
    private Vertex edgeStartVertex = null;

    public MainController() {
        root = new BorderPane();
        canvas = new Canvas(700, 600);
        renderer = new GraphRenderer(canvas);
        buildUI();
    }

    public BorderPane getRoot() { return root; }

    private void buildUI() {
        root.setTop(buildStatusBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildCenterArea());
    }

    // ─── Status Bar ───
    private HBox buildStatusBar() {
        HBox bar = new HBox(15);
        bar.getStyleClass().add("status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("\u2B21 Density-Aware MST");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(TEXT_PRIMARY));

        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-indicator");
        statusLabel.setTextFill(Color.web(GFG_GREEN));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(title, spacer, statusLabel);
        return bar;
    }

    // ─── Sidebar ───
    private ScrollPane buildSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(280);

        // --- Info Cards ---
        sidebar.getChildren().add(sectionTitle("GRAPH INFO"));
        vertexCountLabel = infoValue("0");
        edgeCountLabel = infoValue("0");
        densityLabel = infoValue("0.0000");
        algoLabel = infoValue("\u2014");
        algoLabel.setWrapText(true);

        sidebar.getChildren().addAll(
            infoCard("Vertices", vertexCountLabel),
            infoCard("Edges", edgeCountLabel),
            infoCard("Density", densityLabel),
            infoCard("Recommended", algoLabel)
        );

        sidebar.getChildren().add(new Separator());

        // --- Input Mode ---
        sidebar.getChildren().add(sectionTitle("INPUT MODE"));

        Button addVertexBtn = styledButton("+ Add Vertex", "btn-secondary");
        addVertexBtn.setMaxWidth(Double.MAX_VALUE);
        addVertexBtn.setOnAction(e -> addRandomVertex());

        Button addEdgeBtn = styledButton("Add Edge Mode", "btn-secondary");
        addEdgeBtn.setMaxWidth(Double.MAX_VALUE);
        addEdgeBtn.setOnAction(e -> {
            isAddingEdge = !isAddingEdge;
            edgeStartVertex = null;
            addEdgeBtn.setText(isAddingEdge ? "Click Two Vertices..." : "Add Edge Mode");
            setStatus(isAddingEdge ? "Click source vertex..." : "Ready");
        });

        Button clearBtn = styledButton("Clear Graph", "btn-danger");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> {
            currentGraph = new Graph();
            renderer.clear();
            updateInfoCards();
            setStatus("Graph cleared");
        });

        sidebar.getChildren().addAll(addVertexBtn, addEdgeBtn, clearBtn);
        sidebar.getChildren().add(new Separator());

        // --- Matrix Input ---
        sidebar.getChildren().add(sectionTitle("MATRIX INPUT"));
        matrixInput = new TextArea();
        matrixInput.setPromptText("Enter adjacency matrix\n(space-separated rows)\ne.g.:\n0 2 3\n2 0 5\n3 5 0");
        matrixInput.setPrefRowCount(5);
        matrixInput.setWrapText(true);

        Button loadMatrixBtn = styledButton("Load Matrix", "btn-primary");
        loadMatrixBtn.setMaxWidth(Double.MAX_VALUE);
        loadMatrixBtn.setOnAction(e -> loadFromMatrix());
        sidebar.getChildren().addAll(matrixInput, loadMatrixBtn);
        sidebar.getChildren().add(new Separator());

        // --- Random Generator ---
        sidebar.getChildren().add(sectionTitle("RANDOM GENERATOR"));
        vertexField = new TextField();
        vertexField.setPromptText("Vertices (e.g. 6)");
        edgeField = new TextField();
        edgeField.setPromptText("Edges (e.g. 10)");
        Button genBtn = styledButton("Generate Random", "btn-primary");
        genBtn.setMaxWidth(Double.MAX_VALUE);
        genBtn.setOnAction(e -> generateRandom());
        sidebar.getChildren().addAll(vertexField, edgeField, genBtn);
        sidebar.getChildren().add(new Separator());

        // --- Sample Graphs ---
        sidebar.getChildren().add(sectionTitle("SAMPLE GRAPHS"));
        ComboBox<String> sampleBox = new ComboBox<>();
        sampleBox.getItems().addAll("Sample Dense 1", "Sample Dense 2", "Sample Sparse 1", "Sample Sparse 2");
        sampleBox.setPromptText("Select sample...");
        sampleBox.setMaxWidth(Double.MAX_VALUE);
        sampleBox.setOnAction(e -> loadSample(sampleBox.getValue()));
        sidebar.getChildren().add(sampleBox);
        sidebar.getChildren().add(new Separator());

        // --- Execute ---
        sidebar.getChildren().add(sectionTitle("EXECUTE"));
        Button runBtn = styledButton("Run MST (Auto)", "btn-primary");
        runBtn.setMaxWidth(Double.MAX_VALUE);
        runBtn.setOnAction(e -> runMST());

        Button benchBtn = styledButton("Benchmark All", "btn-secondary");
        benchBtn.setMaxWidth(Double.MAX_VALUE);
        benchBtn.setOnAction(e -> runBenchmark());
        sidebar.getChildren().addAll(runBtn, benchBtn);

        ScrollPane sp = new ScrollPane(sidebar);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: " + BG_SECONDARY + "; -fx-background: " + BG_SECONDARY + ";");
        sp.setPrefWidth(295);
        return sp;
    }

    // ─── Center Area (Tabs) ───
    private TabPane buildCenterArea() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Graph tab
        Tab graphTab = new Tab("Graph & Visualization");
        Pane canvasPane = new Pane(canvas);
        canvasPane.getStyleClass().add("canvas-container");
        graphTab.setContent(canvasPane);

        // Mouse events: click for edge mode, press/drag/release for vertex dragging
        canvas.setOnMouseClicked(this::handleCanvasClick);
        canvas.setOnMousePressed(e -> renderer.onMousePressed(e.getX(), e.getY()));
        canvas.setOnMouseDragged(e -> renderer.onMouseDragged(e.getX(), e.getY()));
        canvas.setOnMouseReleased(e -> renderer.onMouseReleased());

        // Bind canvas size to container
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        canvas.widthProperty().addListener((o, ov, nv) -> renderer.render());
        canvas.heightProperty().addListener((o, ov, nv) -> renderer.render());

        // Benchmark tab
        Tab benchTab = new Tab("Performance Analysis");
        benchmarkBox = new VBox(10);
        benchmarkBox.setPadding(new Insets(20));
        benchmarkBox.setStyle("-fx-background-color: " + BG_PRIMARY + ";");
        Label benchPlaceholder = new Label("Run 'Benchmark All' to see performance comparison.");
        benchPlaceholder.setTextFill(Color.web(TEXT_MUTED));
        benchmarkBox.getChildren().add(benchPlaceholder);
        ScrollPane benchScroll = new ScrollPane(benchmarkBox);
        benchScroll.setFitToWidth(true);
        benchScroll.setStyle("-fx-background-color: " + BG_PRIMARY + "; -fx-background: " + BG_PRIMARY + ";");
        benchTab.setContent(benchScroll);

        // History tab
        Tab histTab = new Tab("Execution History");
        historyList = new VBox(8);
        historyList.setPadding(new Insets(15));
        historyList.setStyle("-fx-background-color: " + BG_PRIMARY + ";");
        Button clearHistBtn = styledButton("Clear History", "btn-danger");
        clearHistBtn.setOnAction(e -> { historyManager.clearAll(); refreshHistory(); });
        VBox histContainer = new VBox(10, clearHistBtn, historyList);
        histContainer.setPadding(new Insets(15));
        histContainer.setStyle("-fx-background-color: " + BG_PRIMARY + ";");
        ScrollPane histScroll = new ScrollPane(histContainer);
        histScroll.setFitToWidth(true);
        histScroll.setStyle("-fx-background-color: " + BG_PRIMARY + "; -fx-background: " + BG_PRIMARY + ";");
        histTab.setContent(histScroll);

        tabs.getTabs().addAll(graphTab, benchTab, histTab);

        histTab.setOnSelectionChanged(e -> { if (histTab.isSelected()) refreshHistory(); });

        return tabs;
    }

    // ─── Canvas Click Handler ───
    private void handleCanvasClick(MouseEvent e) {
        double x = e.getX(), y = e.getY();

        if (isAddingEdge) {
            Vertex clicked = findVertexAt(x, y);
            if (clicked == null) return;

            if (edgeStartVertex == null) {
                edgeStartVertex = clicked;
                setStatus("Now click destination vertex...");
            } else {
                if (!edgeStartVertex.equals(clicked)) {
                    TextInputDialog dlg = new TextInputDialog("1");
                    dlg.setTitle("Edge Weight");
                    dlg.setHeaderText("Enter weight for edge " + edgeStartVertex.getId() + " -> " + clicked.getId());
                    dlg.showAndWait().ifPresent(w -> {
                        try {
                            double weight = Double.parseDouble(w);
                            currentGraph.addEdge(new Edge(edgeStartVertex, clicked, weight));
                            renderer.render(); updateInfoCards();
                            setStatus("Edge added: " + edgeStartVertex.getId() + " -> " + clicked.getId() + " (w=" + weight + ")");
                        } catch (NumberFormatException ex) {
                            showError("Invalid weight value.");
                        }
                    });
                }
                edgeStartVertex = null;
                isAddingEdge = false;
                setStatus("Ready");
            }
        }
    }

    private Vertex findVertexAt(double x, double y) {
        for (Vertex v : currentGraph.getVertices()) {
            double dx = v.getX() - x, dy = v.getY() - y;
            if (Math.sqrt(dx * dx + dy * dy) <= 20) return v;
        }
        return null;
    }

    // ─── Actions ───
    private void addRandomVertex() {
        String id = "V" + currentGraph.getVertices().size();
        // Position at (0,0) — live simulation will initialize and animate it
        Vertex v = new Vertex(id, 0, 0);
        currentGraph.addVertex(v);
        currentInputMethod = "Visual Editor";
        renderer.drawGraph(currentGraph);
        updateInfoCards();
        setStatus("Added vertex " + id);
    }

    private void loadFromMatrix() {
        String text = matrixInput.getText().trim();
        if (text.isEmpty()) { showError("Matrix input is empty."); return; }
        try {
            String[] rows = text.split("\n");
            int n = rows.length;
            double[][] matrix = new double[n][n];
            for (int i = 0; i < n; i++) {
                String[] vals = rows[i].trim().split("\\s+");
                if (vals.length != n) { showError("Non-square matrix at row " + (i + 1)); return; }
                for (int j = 0; j < n; j++) matrix[i][j] = Double.parseDouble(vals[j]);
            }

            renderer.clear(); // Clear previous graph
            currentGraph = new Graph();
            Vertex[] verts = new Vertex[n];
            for (int i = 0; i < n; i++) {
                verts[i] = new Vertex("V" + i, 0, 0);
                currentGraph.addVertex(verts[i]);
            }
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++)
                    if (matrix[i][j] > 0) currentGraph.addEdge(new Edge(verts[i], verts[j], matrix[i][j]));

            currentInputMethod = "Matrix Input";
            renderer.drawGraph(currentGraph);
            updateInfoCards();
            setStatus("Loaded " + n + "-vertex graph from matrix");
        } catch (NumberFormatException ex) { showError("Non-numeric value in matrix."); }
    }

    private void generateRandom() {
        try {
            int v = Integer.parseInt(vertexField.getText().trim());
            int e = Integer.parseInt(edgeField.getText().trim());
            renderer.clear(); // Clear previous graph
            currentGraph = generator.generateRandom(v, e);
            currentInputMethod = "Random Generator";
            renderer.drawGraph(currentGraph);
            updateInfoCards();
            setStatus("Generated random graph: " + v + " vertices, " + e + " edges");
        } catch (NumberFormatException ex) { showError("Enter valid integer values."); }
          catch (IllegalArgumentException ex) { showError(ex.getMessage()); }
    }

    private void loadSample(String name) {
        if (name == null) return;
        renderer.clear(); // Clear previous graph
        currentGraph = switch (name) {
            case "Sample Dense 1" -> generator.sampleDense1();
            case "Sample Dense 2" -> generator.sampleDense2();
            case "Sample Sparse 1" -> generator.sampleSparse1();
            case "Sample Sparse 2" -> generator.sampleSparse2();
            default -> new Graph();
        };
        currentInputMethod = "Sample Library";
        renderer.drawGraph(currentGraph);
        updateInfoCards();
        setStatus("Loaded: " + name);
    }

    private void runMST() {
        if (currentGraph.getVertices().isEmpty()) { showError("No graph loaded."); return; }
        if (!validator.isConnected(currentGraph)) { showError("MST Failure: Graph is not connected"); return; }

        String algo = selector.select(currentGraph);
        MSTAlgorithm algorithm = "KRUSKAL".equals(algo) ? new KruskalAlgorithm() : new PrimMinHeapAlgorithm();
        MSTResult result = algorithm.compute(currentGraph);
        renderer.drawMST(currentGraph, result);

        double density = densityCalc.calculate(currentGraph);
        algoLabel.setText(selector.explain(currentGraph, density));

        // Save to history
        HistoryItem item = HistoryItem.fromGraph(currentGraph, result, currentInputMethod, density);
        historyManager.save(item);

        setStatus("MST computed: " + result.getAlgorithmName() + " | Weight=" + result.getTotalWeight() + " | Time=" + result.getRuntimeMs() + "ms");
    }

    private void runBenchmark() {
        if (currentGraph.getVertices().isEmpty()) { showError("No graph loaded."); return; }
        if (!validator.isConnected(currentGraph)) { showError("MST Failure: Graph is not connected"); return; }

        MSTAlgorithm[] algos = { new KruskalAlgorithm(), new PrimMinHeapAlgorithm(), new PrimBasicAlgorithm() };
        benchmarkBox.getChildren().clear();

        double density = densityCalc.calculate(currentGraph);
        String recommended = selector.select(currentGraph);

        Label header = new Label("Performance Benchmark Results");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        header.setTextFill(Color.web(GFG_GREEN));

        Label densityInfo = new Label(String.format("Graph: %d vertices, %d edges | Density: %.4f | Recommended: %s",
                currentGraph.getVertices().size(), currentGraph.getEdges().size(), density, recommended));
        densityInfo.setTextFill(Color.web(TEXT_SECONDARY));
        densityInfo.setWrapText(true);

        benchmarkBox.getChildren().addAll(header, densityInfo, new Separator());

        for (MSTAlgorithm algo : algos) {
            MSTResult result = algo.compute(currentGraph);
            HBox card = new HBox(20);
            card.getStyleClass().add("info-card");
            card.setPadding(new Insets(15));
            card.setAlignment(Pos.CENTER_LEFT);

            VBox left = new VBox(4);
            Label name = new Label(result.getAlgorithmName());
            name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            name.setTextFill(Color.web(TEXT_PRIMARY));
            Label badge = new Label(result.getAlgorithmName().contains("Kruskal") ? "SPARSE-OPTIMIZED" : "DENSE-OPTIMIZED");
            badge.getStyleClass().add(result.getAlgorithmName().contains("Kruskal") ? "badge-kruskal" : "badge-prim");
            left.getChildren().addAll(name, badge);

            VBox right = new VBox(4);
            right.setAlignment(Pos.CENTER_RIGHT);
            Label time = new Label(result.getRuntimeMs() + " ms");
            time.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
            time.setTextFill(Color.web(SUCCESS));
            Label weight = new Label("MST Weight: " + result.getTotalWeight());
            weight.setTextFill(Color.web(TEXT_SECONDARY));
            right.getChildren().addAll(time, weight);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            card.getChildren().addAll(left, spacer, right);
            benchmarkBox.getChildren().add(card);
        }
        setStatus("Benchmark complete");
    }

    private void refreshHistory() {
        historyList.getChildren().clear();
        List<HistoryItem> items = historyManager.loadAll();
        if (items.isEmpty()) {
            Label empty = new Label("No history yet. Run an MST to create entries.");
            empty.setTextFill(Color.web(TEXT_MUTED));
            historyList.getChildren().add(empty);
            return;
        }
        for (HistoryItem item : items) {
            HBox card = new HBox(12);
            card.getStyleClass().add("history-item");
            card.setPadding(new Insets(10));
            card.setAlignment(Pos.CENTER_LEFT);

            VBox info = new VBox(3);
            Label ts = new Label(item.getTimestamp());
            ts.setTextFill(Color.web(TEXT_MUTED)); ts.setFont(Font.font(10));
            Label desc = new Label(item.getVertexCount() + "V / " + item.getEdgeCount() + "E — " + item.getSelectedAlgorithm());
            desc.setTextFill(Color.web(TEXT_PRIMARY)); desc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            Label meta = new Label(String.format("Density: %.4f | Weight: %.1f | %dms | %s",
                    item.getDensity(), item.getMstWeight(), item.getRuntimeMs(), item.getInputMethod()));
            meta.setTextFill(Color.web(TEXT_SECONDARY)); meta.setFont(Font.font(11));
            info.getChildren().addAll(ts, desc, meta);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button reloadBtn = styledButton("Reload", "btn-secondary");
            reloadBtn.setOnAction(e -> {
                renderer.clear();
                currentGraph = item.toGraph();
                currentInputMethod = "History Reload";
                renderer.drawGraph(currentGraph);
                updateInfoCards();
                setStatus("Reloaded graph from history");
            });

            card.getChildren().addAll(info, spacer, reloadBtn);
            historyList.getChildren().add(card);
        }
    }

    // ─── Helpers ───

    private void updateInfoCards() {
        int v = currentGraph.getVertices().size();
        int e = currentGraph.getEdges().size();
        double density = densityCalc.calculate(currentGraph);
        vertexCountLabel.setText(String.valueOf(v));
        edgeCountLabel.setText(String.valueOf(e));
        densityLabel.setText(String.format("%.4f", density));
        if (v >= 2 && e > 0) {
            algoLabel.setText(selector.explain(currentGraph, density));
        } else {
            algoLabel.setText("\u2014");
        }
    }

    private void setStatus(String msg) { Platform.runLater(() -> statusLabel.setText(msg)); }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    private Label infoValue(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("card-value");
        return l;
    }

    private VBox infoCard(String title, Label valueLabel) {
        VBox card = new VBox(2);
        card.getStyleClass().add("info-card");
        Label t = new Label(title);
        t.getStyleClass().add("card-title");
        card.getChildren().addAll(t, valueLabel);
        return card;
    }

    private Button styledButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().add(styleClass);
        return btn;
    }
}
