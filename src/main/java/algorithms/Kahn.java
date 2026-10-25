package algorithms;
import graph.Graph;
import metrics.Metrics;
import java.util.ArrayDeque;
import java.util.Arrays;

public final class Kahn {
    private Kahn() {}
    public static int[] order(Graph g, Metrics metrics) {
        g.requireDirected(); int[] degree=g.indegrees(), order=new int[g.n()];
        var queue=new ArrayDeque<Integer>();
        for(int v=0;v<g.n();v++)if(degree[v]==0)queue.add(v);
        int count=0;
        while(!queue.isEmpty()) {
            int u=queue.remove();order[count++]=u;
            for(var e:g.outgoing(u)) {
                metrics.edgeScans++;
                if(--degree[e.to()]==0)queue.add(e.to());
            }
        }
        if(count!=g.n())throw new IllegalArgumentException("Cycle detected: processed "+count+" of "+g.n());
        return order;
    }
    public static boolean isValid(Graph g,int[] order) {
        if(order.length!=g.n())return false;
        int[] pos=new int[g.n()];Arrays.fill(pos,-1);
        for(int i=0;i<order.length;i++) {
            int v=order[i];if(v<0||v>=g.n()||pos[v]!=-1)return false;pos[v]=i;
        }
        for(var e:g.edges())if(pos[e.from()]>=pos[e.to()])return false;
        return true;
    }
}
