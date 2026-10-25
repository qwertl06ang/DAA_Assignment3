package tests;
import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class MstTest {
    private void validateForest(Graph g, Mst.Result result) {
        var d = DisjointSetUnion.makeSet(g.n()); long sum = 0;
        for (var e : result.edges()) { assertTrue(d.union(e.from(), e.to())); sum += e.weight(); }
        assertEquals(result.weight(), sum);
        assertEquals(g.n() - result.components(), result.edges().size());
        assertEquals(result.components(), d.components());
        for (var e : g.edges()) assertEquals(d.find(e.from()), d.find(e.to()));
    }
    @Test void oneHundredConnectedGraphsAgree() {
        Random r = new Random(146);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + r.nextInt(70); Graph g = new Graph(n, false);
            for (int i = 1; i < n; i++) g.addEdge(i, r.nextInt(i), r.nextInt(101) - 50);
            for (int i = 0; i < 5*n; i++) g.addEdge(r.nextInt(n), r.nextInt(n), r.nextInt(101)-50);
            var k = Mst.kruskal(g, new Metrics()); var p = Mst.prim(g, new Metrics());
            assertEquals(k.weight(), p.weight()); assertEquals(n - 1, k.edges().size());
            assertEquals(n - 1, p.edges().size()); validateForest(g, k); validateForest(g, p);
        }
    }
    @Test void tinyGraphsAgreeWithExhaustiveSpanningTrees() {
        Random r = new Random(31);
        for (int t = 0; t < 40; t++) {
            Graph g = new Graph(5, false);
            for (int i=1;i<5;i++) g.addEdge(i,r.nextInt(i),r.nextInt(21)-10);
            for (int i=0;i<5;i++) g.addEdge(r.nextInt(5),r.nextInt(5),r.nextInt(21)-10);
            long best = Long.MAX_VALUE;
            for (int mask=0;mask<(1<<g.m());mask++) {
                if (Integer.bitCount(mask)!=4) continue;
                var d=DisjointSetUnion.makeSet(5); long w=0; boolean valid=true;
                for (int j=0;j<g.m();j++) if ((mask&(1<<j))!=0) {
                    var e=g.edges().get(j); if (!d.union(e.from(),e.to())) valid=false; w+=e.weight();
                }
                if (valid && d.components()==1) best=Math.min(best,w);
            }
            assertEquals(best,Mst.kruskal(g,new Metrics()).weight());
            assertEquals(best,Mst.prim(g,new Metrics()).weight());
        }
    }
    @Test void fileGraphsIncludeLoopsParallelEdgesAndIsolation() throws Exception {
        var g = Graph.read(Path.of("data/disconnected.txt"));
        for (var result : new Mst.Result[]{Mst.kruskal(g,new Metrics()),Mst.prim(g,new Metrics())}) {
            assertEquals(3,result.weight()); assertEquals(3,result.components()); validateForest(g,result);
        }
        var connected=Graph.read(Path.of("data/connected.txt"));
        assertEquals(6,Mst.kruskal(connected,new Metrics()).weight());
        assertEquals(6,Mst.prim(connected,new Metrics()).weight());
    }
    @Test void emptySingletonAndLargeWeights() {
        for (int n : new int[]{0,1,5}) {
            Graph g=new Graph(n,false);
            assertEquals(n,Mst.kruskal(g,new Metrics()).components());
            assertEquals(n,Mst.prim(g,new Metrics()).components());
        }
        Graph g=new Graph(3,false);g.addEdge(0,1,Integer.MAX_VALUE);g.addEdge(1,2,Integer.MAX_VALUE);
        assertEquals(2L*Integer.MAX_VALUE,Mst.kruskal(g,new Metrics()).weight());
        assertEquals(2L*Integer.MAX_VALUE,Mst.prim(g,new Metrics()).weight());
    }
}
