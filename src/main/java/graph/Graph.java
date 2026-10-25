package graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Each logical edge is stored once; undirected edges have two adjacency arcs. */
public final class Graph {
    public record Edge(int from, int to, int weight) {}
    private final boolean directed;
    private final List<List<Edge>> adjacency;
    private final List<Edge> edges = new ArrayList<>();
    private final int[] indegree;
    public Graph(int n, boolean directed) {
        if (n < 0) throw new IllegalArgumentException("Negative vertex count");
        this.directed = directed;
        indegree = new int[n];
        adjacency = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adjacency.add(new ArrayList<>());
    }
    public int n() { return adjacency.size(); }
    public int m() { return edges.size(); }
    public boolean directed() { return directed; }
    public void checkVertex(int v) {
        if (v < 0 || v >= n()) throw new IndexOutOfBoundsException("Vertex: " + v);
    }
    public void addEdge(int u, int v, int w) {
        checkVertex(u); checkVertex(v);
        Edge e = new Edge(u, v, w);
        edges.add(e); adjacency.get(u).add(e); indegree[v]++;
        if (!directed) { adjacency.get(v).add(new Edge(v, u, w)); indegree[u]++; }
    }
    public List<Edge> edges() { return Collections.unmodifiableList(edges); }
    public List<Edge> outgoing(int v) { return Collections.unmodifiableList(adjacency.get(v)); }
    public int[] indegrees() { return indegree.clone(); }
    public Graph reversed() {
        Graph r = new Graph(n(), directed);
        for (Edge e : edges) r.addEdge(e.to(), e.from(), e.weight());
        return r;
    }
    public void requireDirected() {
        if (!directed) throw new IllegalArgumentException("A directed graph is required");
    }
    public void requireUndirected() {
        if (directed) throw new IllegalArgumentException("An undirected graph is required");
    }
    public static Graph read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path).stream().map(String::trim)
            .filter(s -> !s.isEmpty() && !s.startsWith("#")).toList();
        try {
            if (lines.isEmpty()) throw new IllegalArgumentException("Empty graph file");
            String[] h = lines.get(0).split("\\s+");
            if (h.length != 3 || (!h[2].equals("directed") && !h[2].equals("undirected")))
                throw new IllegalArgumentException("Expected: n m directed|undirected");
            int n = Integer.parseInt(h[0]), m = Integer.parseInt(h[1]);
            if (m < 0 || lines.size() != m + 1) throw new IllegalArgumentException("Wrong edge count");
            Graph g = new Graph(n, h[2].equals("directed"));
            for (int i = 1; i <= m; i++) {
                String[] t = lines.get(i).split("\\s+");
                if (t.length != 3) throw new IllegalArgumentException("Expected: u v w");
                g.addEdge(Integer.parseInt(t[0]), Integer.parseInt(t[1]), Integer.parseInt(t[2]));
            }
            return g;
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            throw new IOException("Invalid graph file " + path + ": " + ex.getMessage(), ex);
        }
    }
}
