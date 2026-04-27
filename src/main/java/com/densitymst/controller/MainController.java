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
    private ComboBox<String> sampleBox;
    private Button nextStepBtn;
    private boolean isAddingEdge = false;
    private Vertex edgeStartVertex = null;

    // Step-by-step state
    private MSTResult stepResult = null;
    private int stepIndex = 0;
    private double stepTotalWeight = 0;

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
            matrixInput.setText("");
            updateInfoCards();
            setStatus("Graph cleared");
        });

        sidebar.getChildren().addAll(addVertexBtn, addEdgeBtn, clearBtn);
        sidebar.getChildren().add(new Separator());

        // --- Adjacency Matrix ---
        sidebar.getChildren().add(sectionTitle("ADJACENCY MATRIX"));
        matrixInput = new TextArea();
        matrixInput.setPromptText("Enter adjacency matrix\n(space-separated rows)\ne.g.:\n0 2 3\n2 0 5\n3 5 0");
        matrixInput.setPrefRowCount(6);
        matrixInput.setWrapText(false);
        matrixInput.getStyleClass().add("matrix-input");

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
        sampleBox = new ComboBox<>();
        sampleBox.getItems().addAll("Sample Dense 1", "Sample Dense 2", "Sample Sparse 1", "Sample Sparse 2");
        sampleBox.setPromptText("Select sample...");
        sampleBox.setMaxWidth(Double.MAX_VALUE);
        Button loadSampleBtn = styledButton("Load Graph", "btn-primary");
        loadSampleBtn.setMaxWidth(Double.MAX_VALUE);
        loadSampleBtn.setOnAction(e -> loadSample(sampleBox.getValue()));
        sidebar.getChildren().addAll(sampleBox, loadSampleBtn);
        sidebar.getChildren().add(new Separator());

        // --- Execute ---
        sidebar.getChildren().add(sectionTitle("EXECUTE"));
        Button runBtn = styledButton("Run MST (Auto)", "btn-primary");
        runBtn.setMaxWidth(Double.MAX_VALUE);
        runBtn.setOnAction(e -> runMST());

        Button runKruskalBtn = styledButton("Run Kruskal", "btn-secondary");
        runKruskalBtn.setMaxWidth(Double.MAX_VALUE);
        runKruskalBtn.setOnAction(e -> runSpecificMST("KRUSKAL"));

        Button runPrimBtn = styledButton("Run Prim (Both)", "btn-secondary");
        runPrimBtn.setMaxWidth(Double.MAX_VALUE);
        runPrimBtn.setOnAction(e -> runSpecificMST("PRIM"));

        Button resetBtn = styledButton("Reset MST", "btn-danger");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setOnAction(e -> resetMST());

        Button stepBtn = styledButton("Run Step by Step", "btn-secondary");
        stepBtn.setMaxWidth(Double.MAX_VALUE);
        stepBtn.setOnAction(e -> startStepByStep());

        nextStepBtn = styledButton("Next Step \u25B6", "btn-primary");
        nextStepBtn.setMaxWidth(Double.MAX_VALUE);
        nextStepBtn.setDisable(true);
        nextStepBtn.setOnAction(e -> advanceStep());

        Button benchBtn = styledButton("Benchmark All", "btn-secondary");
        benchBtn.setMaxWidth(Double.MAX_VALUE);
        benchBtn.setOnAction(e -> runBenchmark());
        sidebar.getChildren().addAll(runBtn, runKruskalBtn, runPrimBtn, stepBtn, nextStepBtn, resetBtn, benchBtn);

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
                            renderer.render(); updateInfoCards(); updateMatrixDisplay();
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
        updateMatrixDisplay();
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
            updateMatrixDisplay();
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
            updateMatrixDisplay();
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
        updateMatrixDisplay();
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
        updateMatrixDisplay();

        setStatus("MST computed: " + result.getAlgorithmName() + " | Weight=" + result.getTotalWeight()
                + " | Steps=" + result.getComparisons() + " comparisons");
    }

    private void runSpecificMST(String type) {
        if (currentGraph.getVertices().isEmpty()) { showError("No graph loaded."); return; }
        if (!validator.isConnected(currentGraph)) { showError("MST Failure: Graph is not connected"); return; }

        if ("KRUSKAL".equals(type)) {
            MSTResult result = new KruskalAlgorithm().compute(currentGraph);
            renderer.drawMST(currentGraph, result);
            double density = densityCalc.calculate(currentGraph);
            HistoryItem item = HistoryItem.fromGraph(currentGraph, result, currentInputMethod, density);
            historyManager.save(item);
            setStatus("Kruskal MST: Weight=" + result.getTotalWeight()
                    + " | " + result.getComparisons() + " comparisons");
        } else {
            // Run both Prim variants, display Min-Heap result on canvas
            MSTResult heapResult = new PrimMinHeapAlgorithm().compute(currentGraph);
            MSTResult basicResult = new PrimBasicAlgorithm().compute(currentGraph);
            renderer.drawMST(currentGraph, heapResult);
            double density = densityCalc.calculate(currentGraph);
            HistoryItem item = HistoryItem.fromGraph(currentGraph, heapResult, currentInputMethod, density);
            historyManager.save(item);
            setStatus(String.format("Prim MST: Heap=%s (W=%.0f, %d cmp) | Basic=%s (W=%.0f, %d cmp)",
                    heapResult.getAlgorithmName(), heapResult.getTotalWeight(), heapResult.getComparisons(),
                    basicResult.getAlgorithmName(), basicResult.getTotalWeight(), basicResult.getComparisons()));
        }
        updateMatrixDisplay();
    }

    private void resetMST() {
        if (currentGraph.getVertices().isEmpty()) { setStatus("No graph to reset."); return; }
        // Clear step-by-step state
        stepResult = null;
        stepIndex = 0;
        stepTotalWeight = 0;
        nextStepBtn.setDisable(true);
        // Re-draw graph without MST highlighting — keeps vertices/edges intact
        renderer.drawGraph(currentGraph);
        updateInfoCards();
        setStatus("MST visualization reset — graph preserved");
    }

    private void startStepByStep() {
        if (currentGraph.getVertices().isEmpty()) { showError("No graph loaded."); return; }
        if (!validator.isConnected(currentGraph)) { showError("MST Failure: Graph is not connected"); return; }

        String algo = selector.select(currentGraph);
        MSTAlgorithm algorithm = "KRUSKAL".equals(algo) ? new KruskalAlgorithm() : new PrimMinHeapAlgorithm();
        stepResult = algorithm.compute(currentGraph);
        stepIndex = 0;
        stepTotalWeight = 0;

        renderer.prepareMSTStepping(currentGraph, stepResult);
        nextStepBtn.setDisable(false);

        int totalEdges = stepResult.getMstEdges().size();
        setStatus("Step-by-step: " + stepResult.getAlgorithmName()
                + " | 0/" + totalEdges + " edges | Click 'Next Step ▶' to begin");
    }

    private void advanceStep() {
        if (stepResult == null) return;

        Edge added = renderer.stepNextMSTEdge();
        if (added == null) {
            // All edges revealed
            nextStepBtn.setDisable(true);
            double density = densityCalc.calculate(currentGraph);
            HistoryItem item = HistoryItem.fromGraph(currentGraph, stepResult, currentInputMethod, density);
            historyManager.save(item);
            setStatus("Step-by-step complete: " + stepResult.getAlgorithmName()
                    + " | Total Weight=" + stepResult.getTotalWeight());
            stepResult = null;
            return;
        }

        stepIndex++;
        stepTotalWeight += added.getWeight();
        int totalEdges = stepResult.getMstEdges().size();

        setStatus(String.format("Step %d/%d: Added edge %s — %s (w=%.0f) | Running total: %.0f",
                stepIndex, totalEdges,
                added.getSource().getId(), added.getDestination().getId(),
                added.getWeight(), stepTotalWeight));

        if (stepIndex >= totalEdges) {
            nextStepBtn.setDisable(true);
            double density = densityCalc.calculate(currentGraph);
            HistoryItem item = HistoryItem.fromGraph(currentGraph, stepResult, currentInputMethod, density);
            historyManager.save(item);
            setStatus("Step-by-step complete: " + stepResult.getAlgorithmName()
                    + " | Total Weight=" + stepResult.getTotalWeight());
            stepResult = null;
        }
    }

    private void runBenchmark() {
        if (currentGraph.getVertices().isEmpty()) { showError("No graph loaded."); return; }
        if (!validator.isConnected(currentGraph)) { showError("MST Failure: Graph is not connected"); return; }

        MSTAlgorithm[] algos = { new KruskalAlgorithm(), new PrimMinHeapAlgorithm(), new PrimBasicAlgorithm() };
        benchmarkBox.getChildren().clear();

        int vCount = currentGraph.getVertices().size();
        int eCount = currentGraph.getEdges().size();
        double density = densityCalc.calculate(currentGraph);
        String recommended = selector.select(currentGraph);

        Label header = new Label("Performance Benchmark Results");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        header.setTextFill(Color.web(GFG_GREEN));

        Label densityInfo = new Label(String.format("Graph: %d vertices, %d edges | Density: %.4f | Recommended: %s",
                vCount, eCount, density, recommended));
        densityInfo.setTextFill(Color.web(TEXT_SECONDARY));
        densityInfo.setWrapText(true);

        benchmarkBox.getChildren().addAll(header, densityInfo, new Separator());

        for (MSTAlgorithm algo : algos) {
            MSTResult result = algo.compute(currentGraph);

            // --- Algorithm Card ---
            VBox card = new VBox(8);
            card.getStyleClass().add("info-card");
            card.setPadding(new Insets(15));

            // Title row
            HBox titleRow = new HBox(12);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label(result.getAlgorithmName());
            name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            name.setTextFill(Color.web(TEXT_PRIMARY));
            Label badge = new Label(result.getAlgorithmName().contains("Kruskal") ? "SPARSE-OPTIMIZED" : "DENSE-OPTIMIZED");
            badge.getStyleClass().add(result.getAlgorithmName().contains("Kruskal") ? "badge-kruskal" : "badge-prim");
            Region titleSpacer = new Region();
            HBox.setHgrow(titleSpacer, Priority.ALWAYS);
            Label weightLabel = new Label("MST Weight: " + result.getTotalWeight());
            weightLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            weightLabel.setTextFill(Color.web(SUCCESS));
            titleRow.getChildren().addAll(name, badge, titleSpacer, weightLabel);

            // Metrics row
            HBox metricsRow = new HBox(12);
            metricsRow.setAlignment(Pos.CENTER_LEFT);
            metricsRow.getChildren().add(metricChip("Comparisons", result.getComparisons()));
            metricsRow.getChildren().add(metricChip("Heap/Sort Ops", result.getHeapOperations()));
            if (result.getUnionFindOps() > 0) {
                metricsRow.getChildren().add(metricChip("Union-Find Ops", result.getUnionFindOps()));
            }
            if (result.getKeyUpdates() > 0) {
                metricsRow.getChildren().add(metricChip("Key Updates", result.getKeyUpdates()));
            }
            String timeStr = result.getRuntimeMs() == 0 ? "<1 ms" : result.getRuntimeMs() + " ms";
            metricsRow.getChildren().add(metricChip("Wall Time", timeStr));

            // Amortized complexity row
            String amortized = getAmortizedComplexity(result.getAlgorithmName(), vCount, eCount);
            Label complexityLabel = new Label("\u23F1 Amortized: " + amortized);
            complexityLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
            complexityLabel.setTextFill(Color.web(GFG_GREEN));
            complexityLabel.setWrapText(true);
            VBox complexityBox = new VBox(2, complexityLabel);
            complexityBox.getStyleClass().add("complexity-card");

            card.getChildren().addAll(titleRow, metricsRow, complexityBox);
            benchmarkBox.getChildren().add(card);
        }

        // Summary comparison
        benchmarkBox.getChildren().add(new Separator());
        double logV = Math.ceil(Math.log(Math.max(vCount, 2)) / Math.log(2));
        double logE = Math.ceil(Math.log(Math.max(eCount, 2)) / Math.log(2));
        Label summaryTitle = new Label("\u2139 Amortized Complexity Summary");
        summaryTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        summaryTitle.setTextFill(Color.web(TEXT_PRIMARY));
        Label summaryText = new Label(String.format(
                "For V=%d, E=%d:\n" +
                "\u2022 Kruskal:  O(E log E) = O(%d\u00B7%.0f) \u2248 %d ops  [DSU \u2248 free via \u03B1(V)]\n" +
                "\u2022 Prim (Heap): O((V+E) log V) = O((%d+%d)\u00B7%.0f) \u2248 %d ops\n" +
                "\u2022 Prim (Basic): O(V\u00B2) = O(%d\u00B2) = %d ops",
                vCount, eCount,
                eCount, logE, (long)(eCount * logE),
                vCount, eCount, logV, (long)((vCount + eCount) * logV),
                vCount, vCount * vCount));
        summaryText.setFont(Font.font("Consolas", 12));
        summaryText.setTextFill(Color.web(TEXT_SECONDARY));
        summaryText.setWrapText(true);
        VBox summaryCard = new VBox(6, summaryTitle, summaryText);
        summaryCard.getStyleClass().add("complexity-card");
        benchmarkBox.getChildren().add(summaryCard);

        setStatus("Benchmark complete — see step metrics & amortized analysis");
    }

    private VBox metricChip(String label, long value) {
        return metricChip(label, String.valueOf(value));
    }

    private VBox metricChip(String label, String value) {
        VBox chip = new VBox(1);
        chip.getStyleClass().add("metric-card");
        Label valLabel = new Label(value);
        valLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        valLabel.setTextFill(Color.web(TEXT_PRIMARY));
        Label nameLabel = new Label(label);
        nameLabel.setFont(Font.font("Segoe UI", 10));
        nameLabel.setTextFill(Color.web(TEXT_MUTED));
        chip.getChildren().addAll(valLabel, nameLabel);
        return chip;
    }

    private String getAmortizedComplexity(String algoName, int v, int e) {
        double logV = Math.ceil(Math.log(Math.max(v, 2)) / Math.log(2));
        double logE = Math.ceil(Math.log(Math.max(e, 2)) / Math.log(2));
        if (algoName.contains("Kruskal")) {
            // Dominated by sort: O(E log E). DSU with path compression + union by rank
            // is amortized O(E · α(V)) where α ≤ 1 for practical V — effectively free.
            long sortCost = e > 1 ? (long)(e * logE) : 0;
            return String.format("O(E log E) = O(%d\u00B7%.0f) \u2248 %d amortized ops  [DSU \u2248 O(E\u00B7\u03B1) \u2248 free]",
                    e, logE, sortCost);
        } else if (algoName.contains("Min-Heap")) {
            // Each vertex extracted from PQ: V·log(V), each edge may cause insertion: E·log(V)
            long cost = (long)((v + e) * logV);
            return String.format("O((V+E) log V) = O((%d+%d)\u00B7%.0f) \u2248 %d amortized ops",
                    v, e, logV, cost);
        } else {
            return String.format("O(V\u00B2) = O(%d\u00B2) = %d amortized ops", v, (long)v * v);
        }
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
                updateMatrixDisplay();
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

    /**
     * Builds an adjacency matrix from the current graph and displays it
     * in the sidebar matrix TextArea with right-justified columns.
     */
    private void updateMatrixDisplay() {
        List<Vertex> verts = currentGraph.getVertices();
        int n = verts.size();
        if (n == 0) {
            matrixInput.setText("");
            return;
        }

        // Map vertex IDs to indices
        Map<String, Integer> idMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            idMap.put(verts.get(i).getId(), i);
        }

        // Build matrix
        int[][] matrix = new int[n][n];
        for (Edge e : currentGraph.getEdges()) {
            int si = idMap.get(e.getSource().getId());
            int di = idMap.get(e.getDestination().getId());
            int w = (int) e.getWeight();
            matrix[si][di] = w;
            matrix[di][si] = w;
        }

        // Find max width for alignment
        int maxWidth = 1;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                maxWidth = Math.max(maxWidth, String.valueOf(matrix[i][j]).length());

        // Format with right-justified columns
        StringBuilder sb = new StringBuilder();
        String fmt = "%" + (maxWidth + 1) + "d";
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (j > 0) sb.append(" ");
                sb.append(String.format(fmt, matrix[i][j]).stripLeading());
                // Pad to align (use fixed width)
            }
            if (i < n - 1) sb.append("\n");
        }

        matrixInput.setText(sb.toString());
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
