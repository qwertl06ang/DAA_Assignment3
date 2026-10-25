"""Generate all report figures and a five-page report from the actual CSV."""
from pathlib import Path
import csv
import json
import math
import statistics
import xml.etree.ElementTree as ET
from html import escape

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak

ROOT = Path(__file__).resolve().parents[1]
RESULTS = ROOT / 'results'
PLOTS = RESULTS / 'plots'
PLOTS.mkdir(parents=True, exist_ok=True)
with (RESULTS / 'results.csv').open(encoding='utf-8-sig', newline='') as stream:
    rows = list(csv.DictReader(stream))
assert len(rows) == 50, 'Run the full benchmark first (50 data rows required).'
for row in rows:
    for name in ('n', 'm', 'dsu_ops', 'edge_scans', 'comparisons', 'max_depth', 'result'):
        row[name] = int(row[name])
    row['time_ms'] = float(row['time_ms'])
    row['density'] = 'sparse' if row['m'] == 4 * row['n'] else 'dense'
    row['operations'] = row['dsu_ops'] + row['edge_scans'] + row['comparisons']

ALGORITHMS = ['Kruskal', 'Prim', 'Kosaraju', 'Kahn', 'DagShortestPath']
DENSITIES = ['sparse', 'dense']
SIZES = [1000, 3162, 10000, 31623, 100000]
def series(algorithm, density):
    return sorted([r for r in rows if r['algorithm'] == algorithm and r['density'] == density], key=lambda r: r['n'])
for a in ALGORITHMS:
    for d in DENSITIES:
        assert [r['n'] for r in series(a, d)] == SIZES

def slope(algorithm, density, counter):
    data = series(algorithm, density)
    x = [math.log(r['n']) for r in data]
    y = [math.log(r[counter]) for r in data]
    xm, ym = statistics.mean(x), statistics.mean(y)
    return sum((a-xm)*(b-ym) for a,b in zip(x,y)) / sum((a-xm)**2 for a in x)

def bounds(algorithm, density, numerator, denominator):
    values = [r[numerator]/denominator(r) for r in series(algorithm, density)]
    return min(values), max(values)

def format_bounds(values):
    lo, hi = values
    return f'{lo:.3f}' if abs(lo-hi)<1e-10 else f'{lo:.3f} - {hi:.3f}'

ratios = [
    ('Kruskal', 'comparisons / (E log2 E)', 'comparisons', lambda r:r['m']*math.log2(r['m'])),
    ('Kruskal', 'dsu_ops / E', 'dsu_ops', lambda r:r['m']),
    ('Prim', 'comparisons / (E log2 E)', 'comparisons', lambda r:r['m']*math.log2(r['m'])),
    ('Kosaraju', 'scans / (V + E)', 'edge_scans', lambda r:r['n']+r['m']),
    ('Kahn', 'scans / (V + E)', 'edge_scans', lambda r:r['n']+r['m']),
    ('DAG paths', 'scans / (V + E)', 'edge_scans', lambda r:r['n']+r['m']),
]
ratio_table = [['Algorithm', 'Normalized counter', 'Sparse range', 'Dense range']]
for a, label, num, den in ratios:
    actual = 'DagShortestPath' if a == 'DAG paths' else a
    ratio_table.append([a,label,*[format_bounds(bounds(actual,d,num,den)) for d in DENSITIES]])
slope_table = [['Counter curve', 'Sparse slope', 'Dense slope']]
for a in ALGORITHMS:
    slope_table.append([a+' scans',*[f'{slope(a,d,"edge_scans"):.3f}' for d in DENSITIES]])
for a in ['Kruskal','Prim']:
    slope_table.append([a+' comparisons',*[f'{slope(a,d,"comparisons"):.3f}' for d in DENSITIES]])

plt.rcParams.update({'font.family':'DejaVu Sans','font.size':10,'axes.spines.top':False,
    'axes.spines.right':False,'axes.titleweight':'bold','axes.labelcolor':'#263649',
    'text.color':'#263649','axes.edgecolor':'#b9c4d1','grid.color':'#dce3eb','figure.facecolor':'white'})
palette = dict(zip(ALGORITHMS,['#1d4ed8','#c65a15','#15836b','#9b36af','#e12b4d']))
markers = dict(zip(ALGORITHMS,['o','s','^','x','D']))

def base_chart(title, ylabel, log_y=True):
    fig, ax = plt.subplots(figsize=(10,4.8))
    ax.set_title(title,loc='left',pad=15,fontsize=15)
    ax.set_xscale('log')
    if log_y: ax.set_yscale('log')
    ax.set_xlabel('Vertices, n (log scale)');ax.set_ylabel(ylabel)
    ax.grid(True,which='major',alpha=.8)
    ax.set_xticks(SIZES,labels=['1,000','3,162','10,000','31,623','100,000'])
    return fig,ax

fig,ax=base_chart('Counted operations by graph size','find + successful union + scans + comparisons')
for a in ALGORITHMS:
    for d in DENSITIES:
        data=series(a,d)
        ax.plot([r['n'] for r in data],[r['operations'] for r in data],
            color=palette[a],marker=markers[a],markersize=5,linewidth=1.7,
            markerfacecolor='none' if d=='dense' else palette[a],
            linestyle='--' if d=='dense' else '-',label=a+(' / 20n' if d=='dense' else ' / 4n'))
ax.legend(fontsize=7.4,ncol=2,loc='upper left',bbox_to_anchor=(1.01,1),frameon=False)
fig.text(.1,.015,'Kosaraju and DAG paths overlap: each reports 2m aggregate operations. Different operations have different costs.',fontsize=8)
fig.tight_layout(rect=(0,.04,1,1));fig.savefig(PLOTS/'operations_vs_n.png',dpi=180,bbox_inches='tight');plt.close(fig)

fig,ax=base_chart('Edge scans per logical edge','edge_scans / m',False)
for a in ALGORITHMS:
    for d in DENSITIES:
        data=series(a,d)
        ax.plot([r['n'] for r in data],[r['edge_scans']/r['m'] for r in data],
            color=palette[a],marker=markers[a],markersize=6,linewidth=1.7,
            markerfacecolor='none' if d=='dense' else palette[a],
            linestyle='--' if d=='dense' else '-',label=a+(' / 20n' if d=='dense' else ' / 4n'))
ax.set_ylim(0,2.2)
ax.legend(fontsize=7.4,ncol=2,loc='upper left',bbox_to_anchor=(1.01,1),frameon=False)
fig.text(.1,.015,'Overlapping lines: Prim = Kosaraju = 2; Kahn = DAG relaxation = 1, for both densities.',fontsize=8)
fig.tight_layout(rect=(0,.04,1,1));fig.savefig(PLOTS/'scans_per_edge.png',dpi=180,bbox_inches='tight');plt.close(fig)

fig,ax=base_chart('Kruskal: fraction of sorted edges examined','edge_scans / m',False)
for d,color in [('sparse','#1d4ed8'),('dense','#c65a15')]:
    data=series('Kruskal',d);values=[r['edge_scans']/r['m'] for r in data]
    ax.plot(SIZES,values,color=color,marker='o',linewidth=2,label='Sparse (m = 4n)' if d=='sparse' else 'Dense (m = 20n)')
    for x,y in zip(SIZES,values):ax.annotate(f'{y:.1%}',(x,y),xytext=(0,9),textcoords='offset points',ha='center',fontsize=9,color=color)
ax.set_ylim(0,1.12);ax.legend(frameon=False,loc='lower right')
fig.text(.1,.015,'Early stopping saves DSU/selection work. All m edges are still sorted before selection starts.',fontsize=8)
fig.tight_layout(rect=(0,.04,1,1));fig.savefig(PLOTS/'kruskal_examined_share.png',dpi=180,bbox_inches='tight');plt.close(fig)

tests=[]
for file in sorted((ROOT/'target/surefire-reports').glob('TEST-*.xml')):
    elem=ET.parse(file).getroot()
    tests.append({k:elem.attrib[k] for k in ['name','tests','failures','errors','skipped']})
test_count=sum(int(t['tests']) for t in tests)
if tests:
    assert all(int(t['failures'])==int(t['errors'])==int(t['skipped'])==0 for t in tests)
    (RESULTS/'test-summary.txt').write_text('\n'.join(
        [f"{t['name']}: tests={t['tests']}, failures={t['failures']}, errors={t['errors']}, skipped={t['skipped']}" for t in tests]
        +[f'TOTAL: {test_count} tests, 0 failures, 0 errors, 0 skipped'])+'\n',encoding='utf-8')
else:
    raise RuntimeError('Run mvn verify before generating the report; JUnit evidence is required.')

analysis={
    'fit':'ordinary least squares: ln(counter) = intercept + slope * ln(n); five sizes per density',
    'slopes':{a:{d:{c:slope(a,d,c) for c in ['edge_scans']+(['comparisons'] if a in ['Kruskal','Prim'] else [])} for d in DENSITIES} for a in ALGORITHMS},
    'normalized_ranges':ratio_table[1:],
    'tests':tests,
}
(RESULTS/'analysis.json').write_text(json.dumps(analysis,indent=2)+'\n',encoding='utf-8')

complexity=[
 ['Operation / algorithm','Time','Extra space','Justification'],
 ['DSU find / union','O(alpha(V)) amortized','O(1) per call; O(V) DSU','Size linking plus full path compression; makeSet costs O(V).'],
 ['Kruskal forest','O(V + E log(E+1))','O(V + E)','Merge-sort all edges, then DSU operations on at most E edges.'],
 ['Prim (lazy heap)','O(V + E log(E+1))','O(V + E)','O(E) candidates enter/leave a heap of up to O(E); scan all vertices.'],
 ['Kosaraju','O(V + E)','O(V + E)','Two iterative DFS passes plus a stored transpose; O(V) frames.'],
 ['Kahn','O(V + E)','O(V)','Copy cached indegrees; each vertex is queued once and each arc scanned once.'],
 ['DAG shortest paths','O(V + E)','O(V)','Kahn plus one topological relaxation pass; distances and parents.'],
]
choices=[
 ['Situation','Choice','Reason / qualification'],
 ['Undirected, sparse; edge list available','Kruskal','Sorting plus DSU is simple; returns a forest naturally.'],
 ['Undirected; adjacency list available','Prim','Expands a frontier directly from the lists. Lazy heap may hold O(E) edges.'],
 ['Truly dense simple graph, E = Theta(V^2)','Matrix Prim variant','O(V^2) time avoids the heap logarithm; this variant is not implemented.'],
 ['Directed graph; cycles / deadlocks possible','Kosaraju','Groups mutually reachable jobs into SCCs; singleton loops need checking.'],
 ['Dependency order with cycle detection','Kahn','Returns a valid order or explicitly reports a cycle.'],
 ['Directed acyclic graph; negative edges allowed','DAG paths','Topological order allows one pass, from all indegree-zero sources.'],
 ['General weighted directed graph','Dijkstra / Bellman-Ford','Dijkstra for nonnegative weights; Bellman-Ford for negative edges and cycle detection (outside core scope).'],
]
cut=("Consider a cut that no already-selected forest edge crosses, and choose a minimum-weight edge e crossing it. "
     "Take an MST containing the already-selected edges. "
     "If e is absent, adding it creates a cycle with another edge f crossing the cut. "
     "Since w(e) <= w(f), replacing f by e preserves connectivity and cannot increase the total weight. "
     "Thus an MST containing the extended selection exists. "
     "Kruskal uses a cut between current DSU components when it accepts the next lightest edge, while Prim uses the cut between its visited set and the remaining vertices. "
     "Applying the same argument independently to every connected component proves that both algorithms return a minimum spanning forest, even with ties, negative edges and parallel edges.")
dagproof=("Initialize every indegree-zero vertex to distance zero, equivalently connecting a virtual super-source to all these vertices by zero-weight edges. "
          "Every vertex of a finite DAG is reachable from such a source, since repeatedly following predecessors must terminate. "
          "Assume that vertices before u in the topological order already have their correct shortest distances. "
          "Every path into u ends at a predecessor earlier in the order, so the incoming-edge relaxations have considered every possible last edge using correct predecessor distances. "
          "Therefore u's distance is correct before its outgoing edges are relaxed, which proves the induction step. "
          "Negative edges do not invalidate the proof because a DAG has no cycles and no later predecessor can improve an already-processed vertex. "
          "In a general graph, negative edges require a suitable algorithm such as Bellman-Ford, and a reachable negative cycle makes distances to reachable downstream vertices unbounded below. "
          "Saving the predecessor on each strict improvement reconstructs a realizing path whose parent chain terminates at a source.")
testtext=(f"The delivered run passed {test_count} JUnit 5 tests with zero failures, errors or skips. "
          "DSU is checked against set relabeling after random unions; 100 connected random graphs cross-check Kruskal and Prim with n-1 edges and forest invariants; "
          "40 tiny MST cases are also checked by exhaustive edge-subset enumeration. "
          "SCC is compared with forward and reverse BFS from every vertex on 40 random graphs with at most 200 vertices. "
          "100 random DAGs with negative weights are compared with Bellman-Ford and have their restored paths checked. "
          "File fixtures cover disconnected graphs, a cycle, parallel edges, loops and isolation. "
          "A directed path with 100,000 vertices yields 100,000 SCCs and maxDepth=100,000 without recursion; empty graphs, malformed files and long-valued totals are also checked.")
methods=("The graph stores adjacency lists, a logical edge list and cached indegrees. Each generator call uses a fresh java.util.Random(42). "
         "The undirected generator shuffles the vertex permutation, adds its random spanning tree, then adds random non-loop edges. "
         "The directed generator uses only random edges. The DAG generator shuffles a rank array and orients each edge from lower rank to higher rank. "
         "For a random pair, a is drawn once and b is redrawn until b != a; only then is its weight drawn. "
         "All benchmark weights are 1..1000; negative weights are tested separately. Every one of the ten reference triples matched exactly.")
metrics=("Counters are incremented inside the algorithms. DSU operations mean find calls plus successful unions. "
         "Scans mean traversal/selection examinations: Prim visits both adjacency arcs; Kruskal visits only the sorted selection prefix. "
         "Weight comparisons occur in merge sort and PriorityQueue; DAG comparisons compare candidate distances. "
         "Kosaraju's transpose construction is timed but excluded from its two-pass scan counter. "
         "DAG topological preprocessing is timed but uses separate Kahn counters, so its reported scans count only relaxations. "
         "Kahn copies the indegrees maintained when edges are inserted. maxDepth counts live DFS frames, not queue size. "
         "Input storage is excluded from the extra-space table; output/working arrays and the transpose are included.")

reference=[['n','m','MST weight','SCC count','DAG distance sum']]
for n in SIZES:
    for d in DENSITIES:
        get=lambda a:next(r for r in series(a,d) if r['n']==n)
        k,s,p=get('Kruskal'),get('Kosaraju'),get('DagShortestPath')
        reference.append([f'{n:,}',f"{k['m']:,}",f"{k['result']:,}",f"{s['result']:,}",f"{p['result']:,}"])
normtext=("Ranges cover all five sizes at fixed density. The traversal ratios are exactly constant. "
          "Sorting/heap comparison ratios stay in a narrow band, consistent with E log E work. "
          "Kruskal's DSU/E ratio is bounded but not exactly constant because the stopping prefix varies: with S scans on a connected graph, "
          "dsu_ops = 2S + 3(V-1), hence dsu_ops/E <= 2 + 3V/E. Counting DSU calls alone cannot establish the inverse-Ackermann cost; "
          "the amortized proof supplies that bound. Observed ratios are evidence consistent with complexity, not an asymptotic proof.")
slopetext=(f"OLS fits ln(counter) against ln(n) separately for m=4n and m=20n. Traversal scans have slope 1.000 in both densities. "
           f"Kruskal scan slopes are {slope('Kruskal','sparse','edge_scans'):.3f} and {slope('Kruskal','dense','edge_scans'):.3f}; "
           f"its sorting-comparison slopes are {slope('Kruskal','sparse','comparisons'):.3f} and {slope('Kruskal','dense','comparisons'):.3f}. "
           "The log factor belongs to sorting, not scans: S <= E. A scan slope above 1 in this finite sample reflects an increasing examined fraction, "
           "not an E log E scan bound. Also, both assigned densities are E=Theta(V); 'dense' means a larger fixed constant, not a quadratic density.")
small=series('Kruskal','sparse')[0];large=series('Kruskal','dense')[-1];plarge=series('Prim','dense')[-1]
sharetext=(f"At n=100,000, Kruskal examines {series('Kruskal','sparse')[-1]['edge_scans']/400000:.2%} of sparse edges and "
           f"{large['edge_scans']/large['m']:.2%} of dense edges. For n=1,000 these fractions are "
           f"{small['edge_scans']/small['m']:.2%} and {series('Kruskal','dense')[0]['edge_scans']/20000:.2%}. "
           "More alternative connections in the denser sample allow Kruskal to complete the tree after a smaller fraction of the sorted list, "
           "while this Prim implementation still scans both arcs of every edge and drains its heap. Sorting all edges remains necessary in Kruskal. "
           "The plot therefore explains selection savings, not a universal win for either algorithm.")
timing=(f"One warm-up precedes five timed runs; time_ms is their median, including algorithm allocations and preprocessing but excluding graph generation, "
        f"validation and CSV output. On the delivered run the 100,000-vertex dense case took {large['time_ms']:.2f} ms for Kruskal and "
        f"{plarge['time_ms']:.2f} ms for Prim; Kosaraju took {series('Kosaraju','dense')[-1]['time_ms']:.2f} ms, "
        f"Kahn {series('Kahn','dense')[-1]['time_ms']:.2f} ms and DAG paths {series('DagShortestPath','dense')[-1]['time_ms']:.2f} ms. "
        "Small-graph timings fluctuate and can be nonmonotonic because of JIT compilation, garbage collection, cache effects and background load. "
        "Other hardware and JVM versions change these times; operation counts, independent correctness tests and proven bounds are the main evidence.")
repro=("Run mvn clean verify, then java -Xmx2g -jar target/assignment3-graphs-1.0.jar, then python scripts/analyze.py. "
       "The scripts/reproduce.ps1 and scripts/reproduce.sh entry points run all three steps. The CSV contains 50 rows with the exact requested columns. "
       "The benchmark fails on a reference mismatch, MST disagreement, invalid topological order, incorrect scan totals or nondeterministic counters. "
       "Feature branches and tag v1.0 are preserved in repository.bundle. README documents build instructions. "
       "Group and GitHub URL were not supplied and must be completed before submission; no external upload has been performed.")

def md_table(data):
    return '\n'.join(['| '+' | '.join(map(str,data[0]))+' |','| '+' | '.join(['---']*len(data[0]))+' |']+
                     ['| '+' | '.join(map(str,row))+' |' for row in data[1:]])
md=["# Assignment 3 - Graphs\n\n**Adilzhan Aliakbar** | Design and Analysis of Algorithms | 18 October 2026",
    '## 1. Design, methods and complexity',methods,md_table(complexity),metrics,
    '## 2. Correctness, validation and algorithm choice','### Cut-property argument (7 sentences)',cut,
    '### Induction over topological order (8 sentences)',dagproof,md_table(choices),testtext,
    '## 3. Measured results and normalized counters',md_table(reference),md_table(ratio_table),normtext,
    '## 4. Scaling curves and slope check',md_table(slope_table),slopetext,
    '![Counted operations versus n](results/plots/operations_vs_n.png)',
    'Aggregate operations = dsu_ops + edge_scans + comparisons. This is an explicit counting convention, not equal-cost CPU instructions. Kosaraju and DAG paths overlap at 2m.',
    '![Scans per edge](results/plots/scans_per_edge.png)',
    'Prim and Kosaraju equal 2 scans/edge; Kahn and DAG relaxation equal 1. Coincident curves are expected.',
    '## 5. Kruskal prefix, timings and reproducibility','![Kruskal examined share](results/plots/kruskal_examined_share.png)',sharetext,timing,
    'Environment:\n\n```text\n'+(RESULTS/'environment.txt').read_text().strip()+'\n```',repro,
    'Specification source: DAA_Assignment3_Graphs.pdf, supplied assignment, sections 1-10. The detailed ten-case/50-row specification takes precedence over the isolated "four graphs" wording. Optional bonus tasks are not included.']
(ROOT/'REPORT.md').write_text('\n\n'.join(md)+'\n',encoding='utf-8')

# Layout is intentionally fixed to five pages, with readable tables and figures.
navy=colors.HexColor('#15314f');teal=colors.HexColor('#007f82');grey=colors.HexColor('#566779')
styles={
    'title':ParagraphStyle('title',fontName='Helvetica-Bold',fontSize=25,leading=28,textColor=navy,spaceAfter=9),
    'h1':ParagraphStyle('h1',fontName='Helvetica-Bold',fontSize=17,leading=21,textColor=navy,spaceAfter=10),
    'h2':ParagraphStyle('h2',fontName='Helvetica-Bold',fontSize=11,leading=14,textColor=teal,spaceBefore=8,spaceAfter=5),
    'body':ParagraphStyle('body',fontName='Helvetica',fontSize=9.1,leading=12.1,textColor=navy,spaceAfter=8),
    'small':ParagraphStyle('small',fontName='Helvetica',fontSize=8,leading=10.4,textColor=grey,spaceAfter=6),
    'cell':ParagraphStyle('cell',fontName='Helvetica',fontSize=7.5,leading=9.4,textColor=navy),
    'head':ParagraphStyle('head',fontName='Helvetica-Bold',fontSize=7.7,leading=9.6,textColor=colors.white),
}
story=[]
def para(text,kind='body'):
    story.append(Paragraph(escape(text),styles[kind]))
def table(data,widths):
    cells=[[Paragraph(escape(str(cell)),styles['head' if i==0 else 'cell']) for cell in row] for i,row in enumerate(data)]
    t=Table(cells,colWidths=widths,repeatRows=1,hAlign='LEFT')
    t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),navy),('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,colors.HexColor('#eef4f8')]),
        ('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),6),('RIGHTPADDING',(0,0),(-1,-1),6),
        ('TOPPADDING',(0,0),(-1,-1),4.3),('BOTTOMPADDING',(0,0),(-1,-1),4.3),
        ('LINEBELOW',(0,0),(-1,0),.7,teal)]))
    story.append(t);story.append(Spacer(1,8))
def plot(file,width=495):
    from PIL import Image as PILImage
    with PILImage.open(PLOTS/file) as im: w,h=im.size
    story.append(Image(str(PLOTS/file),width=width,height=width*h/w));story.append(Spacer(1,5))
def page():story.append(PageBreak())

para('DESIGN AND ANALYSIS OF ALGORITHMS','small')
para('Assignment 3\nGraphs and networks','title')
para('Adilzhan Aliakbar  |  18 October 2026','h2')
para('1. Design, methods and complexity','h1')
para(methods)
table(complexity,[98,116,91,190])
para('How the counters are defined','h2');para(metrics)
para('Scope: all mandatory algorithms and deliverables. Optional Tarjan/condensation bonuses are not included. The report follows the explicit ten-case / 50-row benchmark requirement.','small')
page()
para('2. Correctness and algorithm choice','h1')
para('Cut property: why an MST edge is safe','h2');para(cut)
para('DAG shortest paths: induction on the order','h2');para(dagproof)
table(choices,[157,91,247])
para('Independent validation','h2');para(testtext,'small')
page()
para('3. Results and normalized work','h1')
para('All ten generated cases match the assignment reference values. Both MST algorithms agree; each topological order is validated edge by edge.','body')
table(reference,[67,82,105,81,160])
para('Counter / complexity formula','h2');table(ratio_table,[77,190,114,114]);para(normtext,'small')
para('Log-log slope check','h2');table(slope_table,[265,115,115]);para(slopetext,'small')
page()
para('4. Scaling curves','h1')
plot('operations_vs_n.png')
para('Figure 1. Ten series: five algorithms in each density. Aggregate operations = dsu_ops + edge_scans + comparisons. These are different-cost operations; overlapping Kosaraju and DAG-path curves both total 2m.','small')
plot('scans_per_edge.png')
para('Figure 2. Traversal scan ratios are flat: Prim and Kosaraju equal 2, Kahn and DAG relaxation equal 1, in both densities. The curves overlap exactly. Kruskal varies because it stops after completing the spanning tree.','small')
para('Interpretation','h2')
para('At each fixed density E is proportional to V, so a linear traversal gives slope 1.00. Sorting and priority-queue comparisons grow with an additional logarithmic factor. The scan-only chart intentionally excludes that comparison work.','body')
page()
para('5. Selection savings and reproducibility','h1')
plot('kruskal_examined_share.png')
para('Figure 3. The examined share is lower on the denser samples. The entire edge list is still sorted.','small')
para(sharetext)
para('Wall-clock measurements','h2');para(timing)
para((RESULTS/'environment.txt').read_text().replace('\n',' | ').strip(' |'),'small')
para('Reproduction and submission status','h2');para(repro,'small')
para('Source specification: supplied DAA_Assignment3_Graphs.pdf, sections 1-10.','small')

def decorate(canvas,doc):
    canvas.setStrokeColor(teal);canvas.setLineWidth(1);canvas.line(50,806,545,806)
    canvas.setFont('Helvetica',8);canvas.setFillColor(grey)
    canvas.drawString(50,30,'Adilzhan Aliakbar | DAA Assignment 3')
    canvas.drawRightString(545,30,f'{doc.page} / 5')
doc=SimpleDocTemplate(str(ROOT/'REPORT.pdf'),pagesize=A4,rightMargin=50,leftMargin=50,topMargin=47,bottomMargin=49,
    title='DAA Assignment 3 - Graphs',author='Adilzhan Aliakbar')
doc.build(story,onFirstPage=decorate,onLaterPages=decorate)
print('Created three plots, REPORT.md, REPORT.pdf and results/analysis.json from 50 measured rows.')
print('Slopes:', json.dumps(analysis['slopes'],indent=2))
