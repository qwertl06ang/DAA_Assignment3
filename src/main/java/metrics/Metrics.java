package metrics;

/** Counters are incremented at the operation, never inferred from graph size. */
public final class Metrics {
    public long findCalls, successfulUnions, edgeScans, comparisons;
    public int maxDepth;
    public long dsuOps() { return findCalls + successfulUnions; }
    public void depth(int depth) { maxDepth = Math.max(maxDepth, depth); }
    public String signature() {
        return dsuOps() + "," + edgeScans + "," + comparisons + "," + maxDepth;
    }
}
