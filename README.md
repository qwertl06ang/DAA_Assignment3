# Assignment 3 - Graphs: DSU, MST, SCC and Shortest Paths

**Student:** Adilzhan Aliakbar  
**Group:** SE-2521  
**GitHub repository:** https://github.com/qwertl06ang/DAA_Assignment3  
**Release:** `main`, tag `v1.0`.

## Build and test

Requires JDK 17 or later and Maven 3.9 or later. Set `JAVA_HOME` to your JDK and add Maven's `bin` folder to `PATH`.
From this project's root:

```sh
mvn clean verify
java -Xmx2g -jar target/assignment3-graphs-1.0.jar
```

The first command builds the runnable JAR and runs JUnit 5 tests. The second runs the full benchmark: ten size/density pairs, three graph kinds per pair, five algorithm rows per pair, 50 CSV rows. It performs one warm-up and takes the median of five measured runs per algorithm/graph. A failed reference check, MST disagreement, invalid ordering, inconsistent counter or wrong row count terminates with a nonzero exit code. `--quick` runs the two 1,000-vertex cases and writes `results/quick.csv` without replacing the full CSV.

The submission ZIP also includes a prebuilt `bin/assignment3-graphs-1.0.jar`. With JDK 17+ installed, it can be run directly using `java -Xmx2g -jar bin/assignment3-graphs-1.0.jar`. Rebuild from source for the defense and to run the tests.

For IntelliJ IDEA: open `pom.xml` as a Maven project, choose an installed JDK >=17, run Maven `verify`, then run `benchmark.Benchmark` with this project as the working directory and `-Xmx2g` as a VM option.

## Reproduce the report and plots

Requires Python 3.10+ plus the plotting/report dependencies:

```sh
python -m pip install -r scripts/requirements.txt
```

Then use **one command**:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/reproduce.ps1
```

On Linux/macOS: `sh scripts/reproduce.sh`. These commands build/test, run the benchmark, and regenerate all three plots, `REPORT.md`, `REPORT.pdf`, and `results/analysis.json`. Operation counts and results are deterministic for this implementation; elapsed times change. PriorityQueue's internal comparator-call count can depend on its JDK implementation; all delivered counts were measured on the JDK recorded in `results/environment.txt`. The explicitly implemented merge sort does not depend on the JDK sorting implementation.

## Run the input examples and print paths

```sh
java -cp target/assignment3-graphs-1.0.jar benchmark.Demo data/disconnected.txt
java -cp target/assignment3-graphs-1.0.jar benchmark.Demo data/cyclic.txt
java -cp target/assignment3-graphs-1.0.jar benchmark.Demo data/dag-negative.txt
```

Input: `n m directed|undirected`, followed by exactly `m` lines `u v w`. Vertex IDs are `0..n-1`; weights are signed Java `int`. Blank lines and `#` comments are accepted. Distances and forest totals use `long`. Parallel edges, self-loops, isolated vertices and empty graphs are supported. An MST algorithm returns a forest on disconnected input. Kahn and DAG shortest paths reject cyclic input with an explicit exception. Source vertices (all indegree-zero vertices) start at distance 0; `pathTo(v)` returns a list of vertex IDs.

## Layout

| Location | Contents |
|---|---|
| `src/main/java/graph` | One adjacency-list graph and file parser |
| `src/main/java/algorithms` | DSU, Kruskal, Prim, Kosaraju, Kahn, DAG shortest paths |
| `src/main/java/metrics` | Counters incremented at the operations |
| `src/main/java/benchmark` | Exact seeded generator, self-checking benchmark, demo |
| `src/test/java/tests` | JUnit 5 tests and independent reference checks |
| `data` | Connected, disconnected, cyclic, and negative-weight DAG examples |
| `results/results.csv` | 50 measured rows, required schema |
| `results/plots` | Three PNG plots |
| `results/analysis.json` | Fitted slopes and normalized counter ranges |
| `results/benchmark.log`, `results/test-summary.txt` | Actual run evidence |
| `REPORT.md`, `REPORT.pdf` | Report with five-page PDF layout |
| `DEFENSE_RU.md` | Russian notes for understanding and explaining the code |

## Metric conventions and specification clarifications

* `dsu_ops = findCalls + successfulUnions`; unsuccessful unions add no union count but their two `find` calls still count. Kruskal explicitly tests the two representatives, then calls `union`, which performs its own two finds.
* `edge_scans` counts algorithm traversal/selection edge examinations. Prim counts both adjacency arcs of each undirected edge. Kruskal counts only the sorted prefix until one component remains, or all edges for a disconnected graph; sorting is represented by `comparisons`.
* Kosaraju's two DFS passes count `2m`; constructing the transpose is included in time/space but excluded from this traversal counter, matching the assignment's prescribed convention. Kahn uses indegrees maintained by the graph, copies them in O(V), and scans `m` edges in its main loop.
* DAG shortest paths include topological preprocessing in elapsed time, but their reported scans count only the `m` relaxations. Kahn's preprocessing counters are separate. `comparisons` counts merge-sort/PriorityQueue weight comparisons or DAG candidate-distance comparisons. A tie-break of vertex IDs is not another weight comparison.
* `max_depth` is the largest number of live explicit DFS frames in either Kosaraju pass; zero for algorithms without DFS. It is not queue size or the JVM call stack.
* The document says "four generated graphs" in its deliverables section but explicitly requires five sizes, two densities and 50 rows elsewhere. This project follows the detailed ten-case/50-row requirement.
* An E log E slope belongs to Kruskal's sorting work, not to `edge_scans`, which is at most E. The report fits both scan and sorting-comparison curves and explains the distinction. Stable ratios support consistency with a bound; measurements alone do not prove asymptotic complexity.
* Optional Tarjan/condensation bonuses are outside this submission's mandatory scope.

## Git history and submission

The working repository contains `main`, `feature/dsu`, `feature/mst`, `feature/scc`, `feature/paths`, `feature/metrics` and tag `v1.0`. Commits were made in these feature branches while implementing the project and merged by fast-forward. The Git author is explicitly `Assignment Builder`, rather than pretending these were unaided student commits.

The ZIP contains a portable `repository.bundle` with the complete history (instead of a platform-specific `.git` directory). To restore it, run from outside the project directory:

```sh
git clone -b main /path/to/repository.bundle assignment3-restored
cd assignment3-restored
git branch feature/dsu origin/feature/dsu
git branch feature/mst origin/feature/mst
git branch feature/scc origin/feature/scc
git branch feature/paths origin/feature/paths
git branch feature/metrics origin/feature/metrics
```


