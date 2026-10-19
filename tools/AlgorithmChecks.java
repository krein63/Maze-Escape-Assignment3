import java.util.*;

/** Extra checks, independent of the three supplied examples.
 * Run: java -cp out tools/AlgorithmChecks.java [--stress]
 * The small oracles deliberately use exhaustive search and Floyd-Warshall.
 */
public class AlgorithmChecks {
    private static int checks;
    private static final long INF = Long.MAX_VALUE / 4;

    public static void main(String[] args) {
        fixedMazes();
        randomMazes();
        fixedSlides();
        randomSlides();
        if (Arrays.asList(args).contains("--stress")) {
            stressMaze();
            stressSlides();
        }
        System.out.println("AlgorithmChecks: " + checks + " checks passed");
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    private static int[] range(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = i;
        return a;
    }

    private static int[][] columns(int[][] edges) {
        int[][] a = new int[3][edges.length];
        for (int i = 0; i < edges.length; i++)
            for (int j = 0; j < 3; j++) a[j][i] = edges[i][j];
        return a;
    }

    private static Input.Walls walls(int r, int c, int[][] edges) {
        int[][] a = columns(edges);
        return new Input.Walls(r, c, a[0], a[1], a[2]);
    }

    private static Input.Slides slides(int n, int[][] edges) {
        int[][] a = columns(edges);
        return new Input.Slides(n, a[0], a[1], a[2]);
    }

    private static long total(Input.Walls w, int[] chosen) {
        long sum = 0;
        for (int i : chosen) sum += w.cost()[i];
        return sum;
    }

    private static void fixedMazes() {
        Input.Walls one = walls(1, 1, new int[0][3]);
        check(Maze.breakWalls(one).length == 0, "Single cell: no broken walls");
        check(Maze.cheapestMaze(one).length == 0 && Maze.primCost(one) == 0,
                "Single cell: zero MST cost");
        Escape.Result r = Escape.escape(one, new int[0]);
        check(Arrays.equals(r.path(), new int[]{0}), "Single cell: path contains start");
        check(r.visited() == 1 && r.deadEnds() == 0, "Single cell: visited/dead ends");

        Input.Walls w = walls(2, 2, new int[][]{
                {0, 1, 100}, {0, 2, 101}, {1, 3, 102}, {2, 3, 1}});
        int[] broken = Maze.breakWalls(w);
        check(Arrays.equals(broken, new int[]{0, 1, 2}), "Mission 1 must keep file order");
        check(total(w, broken) == 303, "File-order cost differs from minimum cost");
        check(total(w, Maze.cheapestMaze(w)) == 202 && Maze.primCost(w) == 202,
                "Both MST algorithms must choose the cheaper fourth wall");
        r = Escape.escape(w, broken);
        check(Arrays.equals(r.path(), new int[]{0, 1, 3}), "DFS: right before down");
        check(r.visited() == 3, "Do not mark the unvisited side branch before exiting");
        check(r.deadEnds() == 2, "Count start/exit by degree, with no special exclusion");

        w = walls(2, 3, new int[][]{
                {0, 3, 1}, {0, 1, 2}, {1, 2, 3}, {3, 4, 4}, {4, 5, 5}});
        r = Escape.escape(w, range(w.count()));
        check(Arrays.equals(r.path(), new int[]{0, 3, 4, 5}), "Backtrack out of dead branch");
        check(r.visited() == 6 && r.deadEnds() == 2, "Visited includes explored dead branch");

        w = walls(1, 4, new int[][]{
                {0, 1, 2_000_000_000}, {1, 2, 1_999_999_999}, {2, 3, 1_999_999_998}});
        check(total(w, Maze.cheapestMaze(w)) == 5_999_999_997L, "Kruskal total uses long");
        check(Maze.primCost(w) == 5_999_999_997L, "Prim total uses long");
    }

    private static void randomMazes() {
        Random random = new Random(73017);
        for (int trial = 0; trial < 36; trial++) {
            int rows = 1 + trial % 3, cols = 1 + (trial / 3) % 3;
            List<int[]> edges = new ArrayList<>();
            for (int u = 0; u < rows * cols; u++) {
                if (u % cols + 1 < cols) edges.add(new int[]{u, u + 1, 0});
                if (u / cols + 1 < rows) edges.add(new int[]{u, u + cols, 0});
            }
            for (int i = 0; i < edges.size(); i++) edges.get(i)[2] = i * 10 + random.nextInt(9) + 1;
            Collections.shuffle(edges, random);
            Input.Walls w = walls(rows, cols, edges.toArray(new int[0][]));
            int[] built = Maze.breakWalls(w), cheapest = Maze.cheapestMaze(w);
            long optimum = exhaustiveMst(w);
            check(built.length == w.cells() - 1 && connected(w, built), "File-order spanning tree");
            check(cheapest.length == w.cells() - 1 && connected(w, cheapest), "Kruskal spanning tree");
            check(total(w, cheapest) == optimum, "Kruskal vs exhaustive spanning trees");
            check(Maze.primCost(w) == optimum, "Prim vs exhaustive spanning trees");
            Escape.Result actual = Escape.escape(w, built);
            List<Integer> path = new ArrayList<>();
            int[] visited = {0}, degree = new int[w.cells()];
            dfsOracle(w, built, 0, new boolean[w.cells()], path, visited);
            for (int e : built) { degree[w.a()[e]]++; degree[w.b()[e]]++; }
            int dead = 0;
            for (int d : degree) if (d == 1) dead++;
            check(Arrays.equals(actual.path(), path.stream().mapToInt(Integer::intValue).toArray()),
                    "Iterative DFS path vs small recursive oracle");
            check(actual.visited() == visited[0] && actual.deadEnds() == dead,
                    "DFS visit order and degrees on narrow/rectangular grids");
        }
    }

    // A plain repeated reachability scan; no DSU or production graph helpers.
    private static boolean connected(Input.Walls w, int[] chosen) {
        boolean[] seen = new boolean[w.cells()];
        seen[0] = true;
        for (int round = 0; round < w.cells(); round++)
            for (int i : chosen)
                if (seen[w.a()[i]] || seen[w.b()[i]]) seen[w.a()[i]] = seen[w.b()[i]] = true;
        for (boolean reached : seen) if (!reached) return false;
        return true;
    }

    private static long exhaustiveMst(Input.Walls w) {
        long best = INF;
        for (int mask = 0; mask < (1 << w.count()); mask++) {
            if (Integer.bitCount(mask) != w.cells() - 1) continue;
            int[] chosen = new int[w.cells() - 1];
            int k = 0;
            for (int e = 0; e < w.count(); e++) if ((mask & (1 << e)) != 0) chosen[k++] = e;
            long cost = total(w, chosen);
            if (cost < best && connected(w, chosen)) best = cost;
        }
        return best;
    }

    // Recursion is used ONLY by this oracle on grids containing at most nine cells.
    private static boolean dfsOracle(Input.Walls w, int[] edges, int u, boolean[] seen,
                                     List<Integer> path, int[] visited) {
        seen[u] = true;
        visited[0]++;
        path.add(u);
        if (u == w.cells() - 1) return true;
        int[] dr = {-1, 0, 1, 0}, dc = {0, 1, 0, -1};
        for (int d = 0; d < 4; d++) {
            int r = u / w.cols() + dr[d], c = u % w.cols() + dc[d];
            if (r < 0 || r >= w.rows() || c < 0 || c >= w.cols()) continue;
            int v = r * w.cols() + c;
            if (seen[v]) continue;
            for (int e : edges) {
                boolean open = (w.a()[e] == u && w.b()[e] == v) || (w.a()[e] == v && w.b()[e] == u);
                if (open && dfsOracle(w, edges, v, seen, path, visited)) return true;
            }
        }
        path.remove(path.size() - 1);
        return false;
    }

    private static void fixedSlides() {
        check(Arrays.equals(checkGraph(slides(1, new int[0][3])), new long[]{1, 0}),
                "Single cell: one flow stage, zero route cost");
        Input.Slides s = slides(6, new int[][]{
                {0, 1, 2_000_000_000}, {1, 0, 2_000_000_000}, {1, 2, 50}, {1, 2, 7},
                {0, 3, 10}, {2, 3, 17}, {3, 2, 19}, {3, 4, 11}, {4, 5, 13}, {0, 5, 99}});
        check(Arrays.equals(checkGraph(s), new long[]{4, 31}), "Parallel minimum and free SCC interiors");
        s = slides(8, new int[][]{{0, 7, 9}, {1, 2, 1}, {2, 3, 1}, {3, 4, 1}, {4, 5, 1}});
        check(Arrays.equals(checkGraph(s), new long[]{5, 9}), "Global longest chain and isolated cell 6");
        checkGraph(slides(6, new int[][]{{0, 5, 3}, {1, 2, 8}, {2, 1, 7}, {3, 4, 4}}));
        s = slides(4, new int[][]{
                {0, 1, 2_000_000_000}, {1, 2, 1_999_999_999}, {2, 3, 1_999_999_998}});
        check(Arrays.equals(checkGraph(s), new long[]{4, 5_999_999_997L}), "Route total uses long");
    }

    private static void randomSlides() {
        Random random = new Random(73019);
        for (int trial = 0; trial < 160; trial++) {
            int n = 1 + trial % 8;
            List<int[]> edges = new ArrayList<>();
            for (int u = 0; u < n; u++)
                for (int v = 0; v < n; v++)
                    if (u != v && random.nextInt(5) == 0) edges.add(new int[]{u, v, 1 + random.nextInt(25)});
            if (n > 1) edges.add(new int[]{0, n - 1, 30}); // Preserve the input's reachable-exit guarantee.
            checkGraph(slides(n, edges.toArray(new int[0][])));
        }
    }

    private static long[] checkGraph(Input.Slides s) {
        int n = s.n();
        boolean[][] reach = new boolean[n][n];
        for (int i = 0; i < n; i++) reach[i][i] = true;
        for (int e = 0; e < s.count(); e++) reach[s.from()[e]][s.to()[e]] = true;
        for (int k = 0; k < n; k++)
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) reach[i][j] |= reach[i][k] && reach[k][j];
        int[] zone = Zones.kosaraju(s);
        check(zone.length == n, "One zone label per cell");
        boolean[] used = new boolean[n];
        int count = 0;
        for (int z : zone) {
            check(z >= 0 && z < n, "SCC label bounds");
            used[z] = true;
            count = Math.max(count, z + 1);
        }
        for (int z = 0; z < count; z++) check(used[z], "SCC labels have no gaps");
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                check((zone[i] == zone[j]) == (reach[i][j] && reach[j][i]), "SCC vs mutual reachability");
        for (int e = 0; e < s.count(); e++)
            check(zone[s.from()[e]] <= zone[s.to()[e]], "SCC labels must be topological");
        long[] actual = Zones.routes(s, zone, count), expected = routeOracle(s, zone, count);
        check(actual.length == 2, "Route result contains two values");
        check(actual[0] == expected[0], "Global flow stages vs all-pairs oracle");
        check(actual[1] == expected[1], "Cheapest zone route vs all-pairs oracle");
        return actual;
    }

    private static long[] routeOracle(Input.Slides s, int[] zone, int n) {
        long[][] distance = new long[n][n];
        int[][] length = new int[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(distance[i], INF);
            distance[i][i] = 0;
            length[i][i] = 1;
        }
        for (int e = 0; e < s.count(); e++) {
            int a = zone[s.from()[e]], b = zone[s.to()[e]];
            if (a == b) continue;
            distance[a][b] = Math.min(distance[a][b], s.time()[e]);
            length[a][b] = 2;
        }
        for (int k = 0; k < n; k++)
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    if (distance[i][k] < INF && distance[k][j] < INF) {
                        distance[i][j] = Math.min(distance[i][j], distance[i][k] + distance[k][j]);
                        length[i][j] = Math.max(length[i][j], length[i][k] + length[k][j] - 1);
                    }
        int stages = 1;
        for (int[] row : length) for (int value : row) stages = Math.max(stages, value);
        return new long[]{stages, distance[zone[0]][zone[s.n() - 1]]};
    }

    private static void stressMaze() {
        int side = 1000, n = side * side;
        int[] path = new int[n - 1];
        int k = 0;
        for (int r = 0; r < side - 2; r++)
            for (int c = 0; c < side; c++) path[k++] = r * side + (r % 2 == 0 ? c : side - 1 - c);
        int top = (side - 2) * side, bottom = (side - 1) * side;
        path[k++] = top;
        path[k++] = bottom;
        path[k++] = bottom + 1;
        for (int c = 2; c < side; c++) {
            path[k++] = (c % 2 == 0 ? bottom : top) + c;
            path[k++] = (c % 2 == 0 ? top : bottom) + c;
        }
        check(k == n - 1, "Stress fixture: maximal corner-to-corner path");
        int[] a = new int[n - 1], b = new int[n - 1], cost = new int[n - 1];
        for (int i = 0; i < n - 2; i++) { a[i] = path[i]; b[i] = path[i + 1]; cost[i] = i + 1; }
        a[n - 2] = top; b[n - 2] = top + 1; cost[n - 2] = n - 1;
        // The omitted cell is a right-hand dead end, visited before continuing downward.
        // A million-cell corner-to-corner simple path is impossible by grid parity.
        Escape.Result r = Escape.escape(new Input.Walls(side, side, a, b, cost), range(n - 1));
        check(Arrays.equals(r.path(), path), "1000x1000 DFS: 999999-cell path, no stack overflow");
        check(r.visited() == n && r.deadEnds() == 3, "1000x1000 DFS: all million cells visited");
        System.out.println("Stress maze: 999999 path cells, 1000000 visited");
    }

    private static void stressSlides() {
        int n = 200_000;
        int[] from = range(n - 1), to = new int[n - 1], time = new int[n - 1];
        for (int i = 0; i < n - 1; i++) { to[i] = i + 1; time[i] = 1; }
        Input.Slides chain = new Input.Slides(n, from, to, time);
        int[] zone = Zones.kosaraju(chain);
        check(Arrays.equals(zone, range(n)), "Deep directed chain: iterative first Kosaraju pass");
        check(Arrays.equals(Zones.routes(chain, zone, n), new long[]{n, n - 1}), "Long DAG route");
        from = Arrays.copyOf(from, n); to = Arrays.copyOf(to, n); time = Arrays.copyOf(time, n);
        from[n - 1] = n - 1; to[n - 1] = 0; time[n - 1] = 1;
        Input.Slides cycle = new Input.Slides(n, from, to, time);
        zone = Zones.kosaraju(cycle);
        check(Arrays.equals(zone, new int[n]), "Deep directed cycle: iterative second Kosaraju pass");
        check(Arrays.equals(Zones.routes(cycle, zone, 1), new long[]{1, 0}), "Large SCC has free travel");
        System.out.println("Stress slides: 200000-cell chain and cycle");
    }
}
