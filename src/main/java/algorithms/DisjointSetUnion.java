package algorithms;

import metrics.Metrics;

public final class DisjointSetUnion {
    private final int[] parent, size;
    private int components;
    private final Metrics metrics;
    private DisjointSetUnion(int n, Metrics metrics) {
        if (n < 0) throw new IllegalArgumentException("Negative size");
        this.metrics = metrics; parent = new int[n]; size = new int[n]; components = n;
        for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }
    }
    public static DisjointSetUnion makeSet(int n, Metrics metrics) {
        return new DisjointSetUnion(n, metrics);
    }
    public static DisjointSetUnion makeSet(int n) { return makeSet(n, new Metrics()); }
    public int find(int x) {
        if (x < 0 || x >= parent.length) throw new IndexOutOfBoundsException("Element: " + x);
        metrics.findCalls++;
        int root = x;
        while (parent[root] != root) root = parent[root];
        while (parent[x] != x) { int next = parent[x]; parent[x] = root; x = next; }
        return root;
    }
    public boolean union(int x, int y) {
        int a = find(x), b = find(y);
        if (a == b) return false;
        if (size[a] < size[b]) { int t = a; a = b; b = t; }
        parent[b] = a; size[a] += size[b]; components--; metrics.successfulUnions++;
        return true;
    }
    public int components() { return components; }
}
