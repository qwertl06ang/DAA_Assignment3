package benchmark;
import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import java.nio.file.Path;
import java.util.Arrays;

public final class Demo {
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Usage: benchmark.Demo data/file.txt");
        Graph g=Graph.read(Path.of(args[0]));
        if(!g.directed()) {
            System.out.println("Kruskal: "+Mst.kruskal(g,new Metrics()));
            System.out.println("Prim:    "+Mst.prim(g,new Metrics()));
        } else {
            var s=Kosaraju.run(g,new Metrics());
            System.out.println("SCC count="+s.count()+"; labels="+Arrays.toString(s.component()));
            try {
                System.out.println("Topological order: "+Arrays.toString(Kahn.order(g,new Metrics())));
                var p=DagShortestPath.run(g,new Metrics());
                for(int v=0;v<g.n();v++)System.out.println(v+": distance="+p.distance()[v]+" path="+p.pathTo(v));
            } catch(IllegalArgumentException ex) { System.out.println(ex.getMessage()); }
        }
    }
}
