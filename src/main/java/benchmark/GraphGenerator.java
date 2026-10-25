package benchmark;
import graph.Graph;
import java.util.Random;

public final class GraphGenerator {
    private GraphGenerator() {}
    public enum Kind { UNDIRECTED, DIRECTED, DAG }
    public static Graph generate(int n,int m,Kind kind) {
        if(n<2 || m<0 || (kind==Kind.UNDIRECTED && m<n-1))
            throw new IllegalArgumentException("Invalid generator dimensions");
        Random rnd=new Random(42);
        Graph g=new Graph(n,kind!=Kind.UNDIRECTED);int[] permutation=new int[n];
        for(int i=0;i<n;i++)permutation[i]=i;
        if(kind!=Kind.DIRECTED)for(int i=n-1;i>0;i--) {
            int j=rnd.nextInt(i+1),t=permutation[i];permutation[i]=permutation[j];permutation[j]=t;
        }
        if(kind==Kind.UNDIRECTED)for(int i=1;i<n;i++)
            g.addEdge(permutation[rnd.nextInt(i)],permutation[i],1+rnd.nextInt(1000));
        while(g.m()<m) {
            int a=rnd.nextInt(n),b;
            do { b=rnd.nextInt(n); } while(b==a);
            if(kind==Kind.DAG && permutation[a]>permutation[b]) {int t=a;a=b;b=t;}
            g.addEdge(a,b,1+rnd.nextInt(1000));
        }
        return g;
    }
}
