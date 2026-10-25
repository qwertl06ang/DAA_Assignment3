package tests;
import algorithms.Kosaraju;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class SccTest {
    private boolean[] bfs(Graph g,int start) {
        boolean[] seen=new boolean[g.n()]; var q=new ArrayDeque<Integer>();
        seen[start]=true;q.add(start);
        while (!q.isEmpty()) for(var e:g.outgoing(q.remove()))
            if(!seen[e.to()]){seen[e.to()]=true;q.add(e.to());}
        return seen;
    }
    @Test void randomGraphsAgreeWithBfsBothDirections() {
        Random r=new Random(19);
        for(int trial=0;trial<40;trial++) {
            int n=1+r.nextInt(200); Graph g=new Graph(n,true);
            for(int i=0;i<3*n;i++)g.addEdge(r.nextInt(n),r.nextInt(n),r.nextInt(10));
            Metrics m=new Metrics();var result=Kosaraju.run(g,m);Graph rev=g.reversed();
            for(int u=0;u<n;u++) {
                boolean[] forward=bfs(g,u),backward=bfs(rev,u);
                for(int v=0;v<n;v++)assertEquals(forward[v]&&backward[v],
                    result.component()[u]==result.component()[v]);
            }
            assertEquals(java.util.Arrays.stream(result.component()).distinct().count(),result.count());
            assertEquals(2L*g.m(),m.edgeScans);
        }
    }
    @Test void pathOfOneHundredThousandIsStackSafe() {
        Graph g=new Graph(100_000,true);
        for(int i=1;i<g.n();i++)g.addEdge(i-1,i,1);
        Metrics m=new Metrics();var s=Kosaraju.run(g,m);
        assertEquals(g.n(),s.count());assertEquals(g.n(),m.maxDepth);
        assertEquals(2L*g.m(),m.edgeScans);
    }
    @Test void cyclicFileAndEmptyGraph() throws Exception {
        Graph g=Graph.read(Path.of("data/cyclic.txt"));var s=Kosaraju.run(g,new Metrics());
        assertEquals(3,s.count());assertEquals(s.component()[0],s.component()[2]);
        assertEquals(s.component()[3],s.component()[4]);assertNotEquals(s.component()[4],s.component()[5]);
        assertEquals(0,Kosaraju.run(new Graph(0,true),new Metrics()).count());
    }
}
