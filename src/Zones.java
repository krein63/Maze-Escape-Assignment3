import java.util.Arrays;

/**
 * Missions 4 and 5. The slides: a directed graph. Cells are 0 .. n-1, slide i goes from s.from()[i] to s.to()[i]
 * and takes s.time()[i]. There are no slides from a cell to itself, and the exit can always be reached from the start.
 *
 * Ready for you: Graphs.slidesFrom(s)[u] is the list of the slides that leave cell u, and Graphs.slidesInto(s)[v] is the list
 * of the slides that enter cell v (reversed graph). The lists hold slide numbers i; the cell at the other end is s.to()[i]
 * or s.from()[i].
 */
public class Zones {

    /**
     * Mission 4. Kosaraju. Returns zoneOf: zoneOf[cell] = number of the zone, numbers 0 .. zones-1.
     * Number the zones in the order in which the second pass finds them. That order is a topological order
     * of the zone graph: every slide between two zones goes from a smaller number to a larger number.
     *
     * Plan: 1) DFS on the slides, write the cells in the order in which they are finished (use a stack, not recursion);
     * 2) go through the cells from the last finished to the first; if a cell has no zone yet, DFS from it on the reversed
     * slides (slidesInto) and give every cell it reaches the next zone number.
     * Is DAG in Main is 1 when the number of zones equals the number of cells (no cycle), so you do not need a separate check.
     */
    public static int[] kosaraju(Input.Slides s) {
        int n = s.n();
        int[][] forward = Graphs.slidesFrom(s);
        int[][] reversed = Graphs.slidesInto(s);
        boolean[] seen = new boolean[n];
        int[] stack = new int[n];
        int[] nextSlide = new int[n];
        int[] finished = new int[n];
        int finishCount = 0;

        // First pass: finish a cell only after all its outgoing slides.
        for (int start = 0; start < n; start++) {
            if (seen[start]) continue;
            int top = 0;
            stack[top] = start;
            nextSlide[top] = 0;
            seen[start] = true;
            while (top >= 0) {
                int cell = stack[top];
                if (nextSlide[top] == forward[cell].length) {
                    finished[finishCount++] = cell;
                    top--;
                } else {
                    int slide = forward[cell][nextSlide[top]++];
                    int next = s.to()[slide];
                    if (!seen[next]) {
                        seen[next] = true;
                        stack[++top] = next;
                        nextSlide[top] = 0;
                    }
                }
            }
        }

        int[] zoneOf = new int[n];
        Arrays.fill(zoneOf, -1);
        int zones = 0;
        // Second pass: reversed slides, from last finished to first.
        for (int i = finishCount - 1; i >= 0; i--) {
            int start = finished[i];
            if (zoneOf[start] != -1) continue;
            int top = 0;
            stack[top] = start;
            zoneOf[start] = zones;
            while (top >= 0) {
                int cell = stack[top--];
                for (int slide : reversed[cell]) {
                    int next = s.from()[slide];
                    if (zoneOf[next] == -1) {
                        zoneOf[next] = zones;
                        stack[++top] = next;
                    }
                }
            }
            zones++;
        }
        return zoneOf;
    }

    /**
     * Mission 5. The graph of zones: the edge X to Y costs the cheapest slide from X to Y, walking inside a zone is free.
     * Go through the zones in the order of their numbers (no priority queue). Returns two numbers:
     * [0] flow stages: the number of zones on the longest chain of zones;
     * [1] the cost of the cheapest route from the zone of cell 0 to the zone of the last cell.
     */
    public static long[] routes(Input.Slides s, int[] zoneOf, int zoneCount) {
        if (zoneCount == 0) return new long[]{0, 0};

        // Store each inter-zone slide in the list of its source zone.
        int[] firstSlide = new int[zoneCount];
        int[] nextSlide = new int[s.count()];
        Arrays.fill(firstSlide, -1);
        for (int slide = 0; slide < s.count(); slide++) {
            int from = zoneOf[s.from()[slide]];
            int to = zoneOf[s.to()[slide]];
            if (from != to) {
                nextSlide[slide] = firstSlide[from];
                firstSlide[from] = slide;
            }
        }

        int[] chain = new int[zoneCount];
        Arrays.fill(chain, 1); // A chain may start in any zone.
        long[] distance = new long[zoneCount];
        Arrays.fill(distance, Long.MAX_VALUE);
        distance[zoneOf[0]] = 0;

        int[] targets = new int[zoneCount];
        int[] bestCost = new int[zoneCount];
        int[] markedBy = new int[zoneCount];
        Arrays.fill(markedBy, -1);
        int flowStages = 1;

        for (int zone = 0; zone < zoneCount; zone++) {
            int targetCount = 0;
            // Keep one edge per target, with the cheapest slide's cost.
            for (int slide = firstSlide[zone]; slide != -1; slide = nextSlide[slide]) {
                int to = zoneOf[s.to()[slide]];
                if (markedBy[to] != zone) {
                    markedBy[to] = zone;
                    targets[targetCount++] = to;
                    bestCost[to] = s.time()[slide];
                } else {
                    bestCost[to] = Math.min(bestCost[to], s.time()[slide]);
                }
            }

            // Every edge goes to a later zone in this topological order.
            flowStages = Math.max(flowStages, chain[zone]);
            for (int i = 0; i < targetCount; i++) {
                int to = targets[i];
                chain[to] = Math.max(chain[to], chain[zone] + 1);
                if (distance[zone] != Long.MAX_VALUE) {
                    distance[to] = Math.min(distance[to], distance[zone] + bestCost[to]);
                }
            }
        }
        return new long[]{flowStages, distance[zoneOf[s.n() - 1]]};
    }
}
