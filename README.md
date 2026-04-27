# Density-Aware Minimum Spanning Tree (MST) Visualizer

A comprehensive, interactive JavaFX application that visualizes Minimum Spanning Tree (MST) algorithms and intelligently selects the most optimal algorithm based on graph density. It features a real-time force-directed graph rendering engine, in-depth performance analysis with step-level metrics, and a full suite of graph creation tools.

## Features

### 1. Intelligent Algorithm Selection
The application calculates the graph's density ($D = \\frac{2E}{V(V-1)}$) and automatically selects the most efficient algorithm:
- **Kruskal's Algorithm** (Optimized with DSU): Selected for **sparse** graphs.
- **Prim's Algorithm** (Optimized with Min-Heap): Selected for **dense** graphs.

### 2. Implemented Algorithms
- **Kruskal's Algorithm**: $O(E \\log E)$ using Disjoint Set Union (DSU) with path compression and union by rank.
- **Prim's Algorithm (Min-Heap)**: $O((V+E) \\log V)$ using a Priority Queue.
- **Prim's Algorithm (Basic)**: $O(V^2)$ utilizing an adjacency matrix.

### 3. Interactive Visualization
- **Force-Directed Layout**: Nodes repel each other while connected edges act as springs, creating a naturally pleasing layout.
- **MST-Only Forces**: Once computed, non-MST edges "lose their force," allowing the tree structure to visually emerge and settle naturally.
- **Step-by-Step Execution**: Manually step through the MST construction, watching edges get added one by one while tracking the running total weight.
- **Reset State**: Clear the MST highlighting while preserving the underlying graph layout.

### 4. Graph Construction Methods
- **Visual Editor**: Click on the canvas to add vertices, and click between two vertices to define an edge and its weight.
- **Adjacency Matrix**: Paste an $N \\times N$ matrix to instantly load a graph. The matrix auto-populates and stays in sync as you visually edit the graph.
- **Random Generator**: Generate random graphs by specifying the desired number of vertices and edges.
- **Sample Library**: Load predefined "Dense" and "Sparse" benchmark graphs.

### 5. Performance Benchmarking
Run all three algorithms simultaneously to compare their efficiency. The benchmark dashboard provides:
- **Wall-clock Time**
- **Step-Level Metrics**: Tracks precise comparisons, heap operations, union-find operations, and key updates.
- **Amortized Complexity Analysis**: Dynamically evaluates the theoretical $O(...)$ notation against the actual $V$ and $E$ of your current graph.

### 6. History Management
Every MST computation is saved to a session history log, allowing you to instantly reload previous graphs, algorithms, and results.

---

## Tech Stack

- **Language**: Java 17
- **UI Framework**: JavaFX 17
- **Build Tool**: Maven

---

## Getting Started

### Prerequisites
- [Java Development Kit (JDK) 17](https://adoptium.net/) or higher.
- [Apache Maven](https://maven.apache.org/) installed and configured in your system PATH.

### Building and Running

1. **Clone or Download the Repository**
   Navigate to the project root directory (where the `pom.xml` is located).

2. **Compile the Project**
   Run the following Maven command to compile the code:
   ```bash
   mvn clean compile
   ```

3. **Run the Application**
   Use the JavaFX Maven plugin to launch the visualizer:
   ```bash
   mvn javafx:run
   ```

---

## How to Use

1. **Create a Graph**: 
   - Click **Add Vertex** and click on the canvas to place nodes.
   - Click **Add Edge**, then click a source node and a destination node to connect them. Enter the weight when prompted.
   - Alternatively, use the **Adjacency Matrix** panel, the **Random Generator**, or select a **Sample Graph** and click "Load Graph".
2. **Compute MST**:
   - Click **Run MST (Auto)** to let the system pick the best algorithm based on density.
   - Click **Run Kruskal** or **Run Prim (Both)** to force a specific algorithm.
   - Click **Run Step by Step** followed by **Next Step** to watch the tree build incrementally.
3. **Analyze Performance**:
   - Click **Benchmark All** to see a side-by-side comparison of operations and amortized complexity for all three algorithms.
4. **Manage State**:
   - Click **Reset MST** to revert to the base graph without clearing vertices/edges.
   - Click **Clear Graph** to start over completely.
   - Use the **History** tab on the right to reload past operations.

---

## Project Structure

- `com.densitymst.core`: Core algorithm logic, density calculators, and algorithm selectors.
- `com.densitymst.model`: Data structures representing Graphs, Vertices, Edges, and results.
- `com.densitymst.view`: JavaFX rendering engine, including the live force-directed simulation.
- `com.densitymst.controller`: UI event handling and application state management.
