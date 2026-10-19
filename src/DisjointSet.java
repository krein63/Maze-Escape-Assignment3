/**
 * Mission 0. Disjoint set union (DSU).
 * Cells are numbered 0 .. n-1. Write the whole class here.
 * Needed: union by size (the smaller tree goes under the larger one) and path compression in find.
 */
public class DisjointSet {
    private final int[] parent;
    private final int[] size;
    private int sets;

    public DisjointSet(int n) {
        if (n < 0) throw new IllegalArgumentException("Size cannot be negative");
        parent = new int[n];
        size = new int[n];
        sets = n;
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }

    /** The root of the set that contains x. */
    public int find(int x) {
        int root = x;
        while (parent[root] != root) root = parent[root];

        // Every cell on this path will point directly to the root.
        while (x != root) {
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    /** Joins the sets of a and b. Returns true if they were different sets, false if they were already one set. */
    public boolean union(int a, int b) {
        int rootA = find(a);
        int rootB = find(b);
        if (rootA == rootB) return false;

        // Attach the smaller set under the larger set.
        if (size[rootA] < size[rootB]) {
            int temp = rootA;
            rootA = rootB;
            rootB = temp;
        }
        parent[rootB] = rootA;
        size[rootA] += size[rootB];
        sets--;
        return true;
    }

    public boolean connected(int a, int b) {
        return find(a) == find(b);
    }

    /** Number of sets now. */
    public int count() {
        return sets;
    }
}
