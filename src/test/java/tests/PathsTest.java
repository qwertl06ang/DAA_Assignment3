package tests;
import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class PathsTest {
    private long[] bellmanFord(Graph g) {
        long[] d=new long[g.n()];Arrays.fill(d,Long.MAX_VALUE);int[] indegree=g.indegrees();
        for(int i=0;i<g.n();i++)if(indegree[i]==0)d[i]=0;
        for(int pass=1;pass<g.n();pass++) {
            boolean changed=false;
            for(var e:g.edges())if(d[e.from()]!=Long.MAX_VALUE && d[e.to()]>d[e.from()]+e.weight()) {
                d[e.to()]=d[e.from()]+e.weight();changed=true;
            }
            if(!changed)break;
        }
        return d;
    }
    @Test void negativeDagPathsAgreeWithBellmanFord() {
        Random r=new Random(33);
        for(int trial=0;trial<100;trial++) {
            int n=2+r.nextInt(70);Graph g=new Graph(n,true);int[] perm=new int[n];
            for(int i=0;i<n;i++)perm[i]=i;
            for(int i=n-1;i>0;i--){int j=r.nextInt(i+1),tmp=perm[i];perm[i]=perm[j];perm[j]=tmp;}
            for(int i=0;i<4*n;i++) {
                int a=r.nextInt(n),b=r.nextInt(n);if(a==b)continue;
                if(a>b){int t=a;a=b;b=t;}g.addEdge(perm[a],perm[b],r.nextInt(101)-50);
            }
            Metrics km=new Metrics(),pm=new Metrics();
            assertTrue(Kahn.isValid(g,Kahn.order(g,km)));
            var result=DagShortestPath.run(g,pm);assertArrayEquals(bellmanFord(g),result.distance());
            assertEquals(g.m(),km.edgeScans);assertEquals(g.m(),pm.edgeScans);
            for(int v=0;v<n;v++) {
                var path=result.pathTo(v);assertEquals(v,path.get(path.size()-1));
                assertEquals(0,g.indegrees()[path.get(0)]);
                for(int k=1;k<path.size();k++) {
                    int u=path.get(k-1),to=path.get(k);
                    assertTrue(g.outgoing(u).stream().anyMatch(e -> e.to()==to &&
                        result.distance()[u]+e.weight()==result.distance()[to]));
                }
            }
        }
    }
    @Test void inputFilesNegativeWeightsAndCycle() throws Exception {
        Graph dag=Graph.read(Path.of("data/dag-negative.txt"));
        var result=DagShortestPath.run(dag,new Metrics());
        assertArrayEquals(new long[]{0,0,2,-3,-2,0},result.distance());
        assertEquals(java.util.List.of(1,2,3,4),result.pathTo(4));
        assertEquals(java.util.List.of(5),result.pathTo(5));
        Graph cyclic=Graph.read(Path.of("data/cyclic.txt"));
        assertThrows(IllegalArgumentException.class,()->Kahn.order(cyclic,new Metrics()));
        assertThrows(IllegalArgumentException.class,()->DagShortestPath.run(cyclic,new Metrics()));
    }
    @Test void selfLoopEmptyGraphAndWideDistances() {
        Graph loop=new Graph(1,true);loop.addEdge(0,0,0);
        assertThrows(IllegalArgumentException.class,()->Kahn.order(loop,new Metrics()));
        assertEquals(0,Kahn.order(new Graph(0,true),new Metrics()).length);
        assertEquals(0,DagShortestPath.run(new Graph(0,true),new Metrics()).sum());
        Graph g=new Graph(3,true);g.addEdge(0,1,Integer.MIN_VALUE);g.addEdge(1,2,Integer.MIN_VALUE);
        assertEquals(2L*Integer.MIN_VALUE,DagShortestPath.run(g,new Metrics()).distance()[2]);
        assertFalse(Kahn.isValid(g,new int[]{0,0,2}));assertFalse(Kahn.isValid(g,new int[]{2,1,0}));
    }
}
