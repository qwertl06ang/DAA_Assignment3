package algorithms;
import graph.Graph;
import metrics.Metrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DagShortestPath {
    private DagShortestPath() {}
    public record Result(long[] distance,int[] parent) {
        public long sum() { long s=0;for(long d:distance)s+=d;return s; }
        public List<Integer> pathTo(int target) {
            if(target<0||target>=distance.length)throw new IndexOutOfBoundsException();
            List<Integer> path=new ArrayList<>();
            for(int v=target;v!=-1;v=parent[v])path.add(v);
            Collections.reverse(path);return List.copyOf(path);
        }
    }
    public static Result run(Graph g,Metrics metrics) {
        // The topological preprocessing is timed, but its scans belong to Kahn.
        int[] order=Kahn.order(g,new Metrics());
        int[] degree=g.indegrees(),parent=new int[g.n()];
        long[] distance=new long[g.n()];Arrays.fill(distance,Long.MAX_VALUE);Arrays.fill(parent,-1);
        for(int v=0;v<g.n();v++)if(degree[v]==0)distance[v]=0;
        for(int u:order) for(var e:g.outgoing(u)) {
            metrics.edgeScans++;
            // Every DAG vertex is reachable from at least one indegree-zero vertex.
            long candidate=distance[u]+e.weight();metrics.comparisons++;
            if(candidate<distance[e.to()]){distance[e.to()]=candidate;parent[e.to()]=u;}
        }
        return new Result(distance,parent);
    }
}
