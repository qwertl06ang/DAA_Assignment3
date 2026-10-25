package benchmark;
import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

public final class Benchmark {
    private Benchmark() {}
    private static final int[] SIZES={1000,3162,10000,31623,100000};
    // Expected results are assertions only; CSV values come from computed outputs.
    private static final long[][] REFERENCE={
        {147970,27,396274},{30363,1,252411},{468911,137,1271911},{97589,1,723157},
        {1486139,373,3941828},{309550,1,2381342},{4715515,1188,12551891},
        {968517,1,7376612},{15012094,3923,40096539},{3056681,1,23512087}};
    private record Measurement(double milliseconds,Metrics metrics,long result) {}
    private static Measurement measure(Function<Metrics,Long> algorithm) {
        // One unmeasured warm-up, then five fresh runs with fresh counters.
        long expected=algorithm.apply(new Metrics());
        double[] times=new double[5];Metrics first=null;
        for(int i=0;i<5;i++) {
            Metrics metrics=new Metrics();long start=System.nanoTime();
            long result=algorithm.apply(metrics);times[i]=(System.nanoTime()-start)/1_000_000.0;
            if(result!=expected)throw new IllegalStateException("Non-deterministic result");
            if(first==null)first=metrics;
            else if(!first.signature().equals(metrics.signature()))
                throw new IllegalStateException("Non-deterministic counters");
        }
        Arrays.sort(times);return new Measurement(times[2],first,expected);
    }
    private static String row(String name,String graph,Graph g,Measurement r) {
        return String.format(Locale.ROOT,"%s,%s,%d,%d,%.6f,%d,%d,%d,%d,%d",
            name,graph,g.n(),g.m(),r.milliseconds(),r.metrics().dsuOps(),r.metrics().edgeScans,
            r.metrics().comparisons,r.metrics().maxDepth,r.result());
    }
    private static void check(long expected,long actual,String label) {
        if(expected!=actual)throw new IllegalStateException(label+": expected "+expected+", got "+actual);
    }
    public static void main(String[] args) throws Exception {
        boolean quick=Arrays.asList(args).contains("--quick");
        List<String> csv=new ArrayList<>();
        csv.add("algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result");
        System.out.println("n,m,MST total weight,SCC count,Sum of DAG distances");
        int index=0;
        for(int n:SIZES) {
            if(quick && n!=1000)break;
            for(int density:new int[]{4,20}) {
                int m=density*n;String label=density==4?"sparse":"dense";
                Graph u=GraphGenerator.generate(n,m,GraphGenerator.Kind.UNDIRECTED);
                Measurement k=measure(metrics -> Mst.kruskal(u,metrics).weight());
                Measurement p=measure(metrics -> Mst.prim(u,metrics).weight());
                check(k.result(),p.result(),"MST disagreement");
                check(REFERENCE[index][0],k.result(),"MST reference n="+n+" m="+m);
                csv.add(row("Kruskal","undirected-"+label,u,k));csv.add(row("Prim","undirected-"+label,u,p));
                Graph d=GraphGenerator.generate(n,m,GraphGenerator.Kind.DIRECTED);
                Measurement s=measure(metrics -> (long)Kosaraju.run(d,metrics).count());
                check(REFERENCE[index][1],s.result(),"SCC reference");check(2L*m,s.metrics().edgeScans,"SCC scans");
                csv.add(row("Kosaraju","directed-"+label,d,s));
                Graph dag=GraphGenerator.generate(n,m,GraphGenerator.Kind.DAG);
                int[] order=Kahn.order(dag,new Metrics());
                if(!Kahn.isValid(dag,order))throw new IllegalStateException("Invalid topological order");
                Measurement t=measure(metrics -> (long)Kahn.order(dag,metrics).length);
                Measurement sp=measure(metrics -> DagShortestPath.run(dag,metrics).sum());
                check(n,t.result(),"Order length");check(m,t.metrics().edgeScans,"Kahn scans");
                check(m,sp.metrics().edgeScans,"DAG relaxation scans");
                check(REFERENCE[index][2],sp.result(),"DAG reference");
                csv.add(row("Kahn","dag-"+label,dag,t));csv.add(row("DagShortestPath","dag-"+label,dag,sp));
                System.out.printf(Locale.ROOT,"%d,%d,%d,%d,%d%n",n,m,k.result(),s.result(),sp.result());index++;
            }
        }
        check(quick?10:50,csv.size()-1,"CSV row count");
        Files.createDirectories(Path.of("results"));
        Path output=Path.of(quick?"results/quick.csv":"results/results.csv");
        Files.write(output,csv);
        System.out.println("PASS: reference values, agreement, order, counters and "+(csv.size()-1)+" rows -> "+output);
        Files.writeString(Path.of("results/environment.txt"),"Java: "+System.getProperty("java.runtime.version")+
            "\nVM: "+System.getProperty("java.vm.name")+"\nOS: "+System.getProperty("os.name")+
            " "+System.getProperty("os.arch")+"\nProcessors: "+Runtime.getRuntime().availableProcessors()+
            "\nWarmups: 1 per algorithm/graph; timed runs: 5; statistic: median\nSeed: new Random(42) per graph\n");
    }
}
