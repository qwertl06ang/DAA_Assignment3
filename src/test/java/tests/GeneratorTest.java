package tests;
import algorithms.*;
import benchmark.GraphGenerator;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GeneratorTest {
    @Test void smallestReferenceGraphs() {
        for(int density:new int[]{4,20}) {
            int m=1000*density;
            Graph u=GraphGenerator.generate(1000,m,GraphGenerator.Kind.UNDIRECTED);
            assertEquals(density==4?147970:30363,Mst.kruskal(u,new Metrics()).weight());
            assertEquals(Mst.kruskal(u,new Metrics()).weight(),Mst.prim(u,new Metrics()).weight());
            Graph d=GraphGenerator.generate(1000,m,GraphGenerator.Kind.DIRECTED);
            assertEquals(density==4?27:1,Kosaraju.run(d,new Metrics()).count());
            Graph dag=GraphGenerator.generate(1000,m,GraphGenerator.Kind.DAG);
            assertEquals(density==4?396274:252411,DagShortestPath.run(dag,new Metrics()).sum());
            assertTrue(Kahn.isValid(dag,Kahn.order(dag,new Metrics())));
        }
    }
}
