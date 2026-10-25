package tests;
import graph.Graph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class GraphTest {
    @TempDir Path temp;
    @Test void rejectsMalformedFiles() throws Exception {
        for(String text:new String[]{"", "2 1 directed", "2 1 other\n0 1 2", "2 1 directed\n0 3 1", "-1 0 directed"}) {
            Path p=temp.resolve("bad.txt");Files.writeString(p,text);
            assertThrows(IOException.class,()->Graph.read(p));
        }
    }
    @Test void adjacencyAndIndegreeCountParallelEdgesAndLoops() {
        Graph g=new Graph(2,false);g.addEdge(0,1,2);g.addEdge(0,1,3);g.addEdge(1,1,4);
        assertEquals(3,g.m());assertEquals(2,g.outgoing(0).size());assertEquals(4,g.outgoing(1).size());
        assertArrayEquals(new int[]{2,4},g.indegrees());
        assertThrows(UnsupportedOperationException.class,()->g.edges().clear());
    }
}
