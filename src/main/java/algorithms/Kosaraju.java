package algorithms;
import graph.Graph;
import metrics.Metrics;
import java.util.Arrays;

public final class Kosaraju {
    private Kosaraju() {}
    public record Result(int[] component, int count) {}
    public static Result run(Graph g, Metrics metrics) {
        g.requireDirected(); int n=g.n(), finished=0;
        boolean[] seen=new boolean[n]; int[] order=new int[n];
        // A frame is (vertex, next outgoing edge index), exactly like recursive DFS.
        int[] vertex=new int[n], next=new int[n];
        for (int root=0;root<n;root++) {
            if (seen[root]) continue;
            int top=0; vertex[0]=root; next[0]=0; seen[root]=true; metrics.depth(1);
            while (top>=0) {
                int u=vertex[top];
                if (next[top]<g.outgoing(u).size()) {
                    int v=g.outgoing(u).get(next[top]++).to(); metrics.edgeScans++;
                    if (!seen[v]) {
                        seen[v]=true; top++; vertex[top]=v; next[top]=0; metrics.depth(top+1);
                    }
                } else { order[finished++]=u; top--; }
            }
        }
        // Transpose construction is preprocessing, not a DFS edgeScan.
        Graph reverse=g.reversed(); int[] component=new int[n]; Arrays.fill(component,-1);
        int count=0;
        for (int i=n-1;i>=0;i--) {
            int root=order[i]; if (component[root]!=-1) continue;
            int top=0; vertex[0]=root; next[0]=0; component[root]=count; metrics.depth(1);
            while (top>=0) {
                int u=vertex[top];
                if (next[top]<reverse.outgoing(u).size()) {
                    int v=reverse.outgoing(u).get(next[top]++).to(); metrics.edgeScans++;
                    if (component[v]==-1) {
                        component[v]=count; top++; vertex[top]=v; next[top]=0; metrics.depth(top+1);
                    }
                } else top--;
            }
            count++;
        }
        return new Result(component,count);
    }
}
