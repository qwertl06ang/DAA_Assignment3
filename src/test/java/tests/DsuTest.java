package tests;
import algorithms.DisjointSetUnion;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class DsuTest {
    @Test void randomUnionsAgreeWithRelabelingReference() {
        int n = 80; int[] label = new int[n];
        for (int i = 0; i < n; i++) label[i] = i;
        Metrics m = new Metrics(); var d = DisjointSetUnion.makeSet(n, m);
        Random r = new Random(917);
        for (int step = 0; step < 500; step++) {
            int a = r.nextInt(n), b = r.nextInt(n), old = label[b], replacement = label[a];
            assertEquals(old != replacement, d.union(a, b));
            for (int i = 0; i < n; i++) if (label[i] == old) label[i] = replacement;
            assertEquals(java.util.Arrays.stream(label).distinct().count(), d.components());
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++)
                assertEquals(label[i] == label[j], d.find(i) == d.find(j));
        }
        assertEquals(n - d.components(), m.successfulUnions);
    }
    @Test void countersAndEmptySet() {
        Metrics m = new Metrics(); var d = DisjointSetUnion.makeSet(3, m);
        assertTrue(d.union(0, 1)); assertFalse(d.union(1, 0)); d.find(2);
        assertEquals(5, m.findCalls); assertEquals(1, m.successfulUnions);
        assertEquals(6, m.dsuOps()); assertEquals(2, d.components());
        assertEquals(0, DisjointSetUnion.makeSet(0).components());
        assertThrows(IndexOutOfBoundsException.class, () -> d.find(3));
    }
}
