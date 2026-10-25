package algorithms;

import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public final class Mst {
    private Mst() {}
    public record Result(long weight, List<Edge> edges, int components) {
        public Result { edges = List.copyOf(edges); }
    }
    public static Result kruskal(Graph g, Metrics metrics) {
        g.requireUndirected();
        Edge[] edges = g.edges().toArray(Edge[]::new);
        mergeSort(edges, metrics);
        var dsu = DisjointSetUnion.makeSet(g.n(), metrics);
        List<Edge> selected = new ArrayList<>(); long total = 0;
        for (Edge e : edges) {
            if (dsu.components() <= 1) break;
            metrics.edgeScans++;
            if (dsu.find(e.from()) != dsu.find(e.to())) {
                dsu.union(e.from(), e.to());
                selected.add(e); total += e.weight();
            }
        }
        return new Result(total, selected, dsu.components());
    }
    /** Stable bottom-up merge sort makes weight-comparison counts explicit. */
    private static void mergeSort(Edge[] a, Metrics metrics) {
        Edge[] tmp = new Edge[a.length];
        for (int width = 1; width < a.length; width *= 2) {
            for (int left = 0; left < a.length; left += 2 * width) {
                int mid = Math.min(left + width, a.length);
                int right = Math.min(left + 2 * width, a.length);
                int i = left, j = mid, k = left;
                while (i < mid && j < right) {
                    metrics.comparisons++;
                    tmp[k++] = a[i].weight() <= a[j].weight() ? a[i++] : a[j++];
                }
                while (i < mid) tmp[k++] = a[i++];
                while (j < right) tmp[k++] = a[j++];
            }
            System.arraycopy(tmp, 0, a, 0, a.length);
        }
    }
    /** Lazy Prim: each component gets a root, starting with vertex zero. */
    public static Result prim(Graph g, Metrics metrics) {
        g.requireUndirected();
        boolean[] visited = new boolean[g.n()];
        List<Edge> selected = new ArrayList<>();
        var pq = new PriorityQueue<Edge>((a, b) -> {
            metrics.comparisons++;
            int c = Integer.compare(a.weight(), b.weight());
            if (c != 0) return c;
            c = Integer.compare(a.from(), b.from());
            return c != 0 ? c : Integer.compare(a.to(), b.to());
        });
        long total = 0; int components = 0;
        for (int root = 0; root < g.n(); root++) {
            if (visited[root]) continue;
            components++; visited[root] = true;
            offerOutgoing(g, root, visited, pq, metrics);
            while (!pq.isEmpty()) {
                Edge e = pq.remove();
                if (visited[e.to()]) continue;
                visited[e.to()] = true; selected.add(e); total += e.weight();
                offerOutgoing(g, e.to(), visited, pq, metrics);
            }
        }
        return new Result(total, selected, components);
    }
    private static void offerOutgoing(Graph g, int v, boolean[] visited,
                                       PriorityQueue<Edge> pq, Metrics metrics) {
        for (Edge e : g.outgoing(v)) {
            metrics.edgeScans++;
            if (!visited[e.to()]) pq.add(e);
        }
    }
}
