import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Missions 1 and 2. Building the maze out of the walls.
 * A wall k joins the cells w.a()[k] and w.b()[k] and costs w.cost()[k]. Indexes start from 0,
 * the line number in walls.txt is the index + 1. The cells are connected by DisjointSet.
 */
public class Maze {

    /**
     * Mission 1. Take the walls in the order of the file. Break a wall when its two cells are in different sets.
     * Returns the indexes of the broken walls, in the order of breaking.
     */
    public static int[] breakWalls(Input.Walls w) {
        DisjointSet sets = new DisjointSet(w.cells());
        int[] broken = new int[Math.max(0, w.cells() - 1)];
        int count = 0;

        for (int wall = 0; wall < w.count() && count < broken.length; wall++) {
            // A successful union joins two parts without creating a cycle.
            if (sets.union(w.a()[wall], w.b()[wall])) {
                broken[count++] = wall;
            }
        }
        return Arrays.copyOf(broken, count);
    }

    /**
     * Mission 2. Kruskal: the same, but take the walls from the cheapest to the most expensive.
     * Returns the indexes of the broken walls.
     */
    public static int[] cheapestMaze(Input.Walls w) {
        Integer[] order = new Integer[w.count()];
        for (int wall = 0; wall < w.count(); wall++) {
            order[wall] = wall;
        }
        Arrays.sort(order, Comparator.comparingInt(wall -> w.cost()[wall]));

        DisjointSet sets = new DisjointSet(w.cells());
        int[] broken = new int[Math.max(0, w.cells() - 1)];
        int count = 0;

        for (int wall : order) {
            if (count == broken.length) break;
            if (sets.union(w.a()[wall], w.b()[wall])) {
                broken[count++] = wall;
            }
        }
        return Arrays.copyOf(broken, count);
    }

    /**
     * Mission 2. Prim with a priority queue. Returns the total cost of the cheapest maze.
     * It must be equal to the total cost of cheapestMaze.
     */
    public static long primCost(Input.Walls w) {
        int n = w.cells();
        if (n <= 1) return 0;

        int[][] incident = incidentWalls(w);
        boolean[] inTree = new boolean[n];
        PriorityQueue<Integer> queue = new PriorityQueue<>(
                Comparator.comparingInt(wall -> w.cost()[wall]));

        inTree[0] = true;
        int reached = 1;
        long total = 0;
        for (int wall : incident[0]) queue.add(wall);

        while (!queue.isEmpty() && reached < n) {
            int wall = queue.remove();
            int a = w.a()[wall];
            int b = w.b()[wall];
            // This wall is now inside the tree, so breaking it would make a cycle.
            if (inTree[a] && inTree[b]) continue;

            int cell = inTree[a] ? b : a;
            inTree[cell] = true;
            reached++;
            total += w.cost()[wall];

            for (int nextWall : incident[cell]) {
                int other = w.a()[nextWall] == cell
                        ? w.b()[nextWall] : w.a()[nextWall];
                if (!inTree[other]) queue.add(nextWall);
            }
        }
        return total;
    }

    /** incident[cell] contains the indexes of all walls touching this cell. */
    private static int[][] incidentWalls(Input.Walls w) {
        int n = w.cells();
        int[] degree = new int[n];
        for (int wall = 0; wall < w.count(); wall++) {
            degree[w.a()[wall]]++;
            degree[w.b()[wall]]++;
        }

        int[][] incident = new int[n][];
        for (int cell = 0; cell < n; cell++) {
            incident[cell] = new int[degree[cell]];
        }

        int[] filled = new int[n];
        for (int wall = 0; wall < w.count(); wall++) {
            int a = w.a()[wall];
            int b = w.b()[wall];
            incident[a][filled[a]++] = wall;
            incident[b][filled[b]++] = wall;
        }
        return incident;
    }
}
